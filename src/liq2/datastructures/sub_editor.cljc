(ns liq2.datastructures.sub-editor
  (:require [clojure.string :as str]))

; https://devhints.io/vimscript-functions
;
; STRATEGY:
; Sub_editor and frameing to work optimal clj + cljs, then fill out the in-between

(defn sub-editor
  [text]
  {::lines (mapv (fn [l] (mapv #(hash-map ::char %) l)) (str/split-lines text))
   ::line-ending :unix
   ::col 1
   ::row 1
   ::mem-col 1                ; Remember column when moving up and down
   ::mode ::normal})          ; This allows cursor to be "after line", like vim. (Separate from major and minor modes!)

(defn insert-in-vector
  [v n elem]
  (into [] (concat
             (into [] (subvec v 0 n))
             [elem]
             (into [] (subvec v n)))))

(defn append-line-at-end
  "Append empty lines at end"
  ([se n]
   (loop [se0 se n0 n]
     (if (<= n0 0)
       se0
       (recur (update se0 ::lines conj []) (dec n0)))))
  ([se] (append-line-at-end se 1)))

(defn append-spaces-to-row
  [se row n]
  (update-in se [::lines (dec row)] #(into [] (concat % (repeat n {::char \space}))))) 

(comment
  (let [se (sub-editor "abcd\nxyz")
        row 4
        spaces 5]
    (append-spaces-to-row se 2 10)
  ))


(defn line-count
  [se]
  (count (se ::lines)))

(defn col-count
  [se row]
  (-> se ::lines (get (dec row)) count))

(defn insert-mode?
  [se]
  (= (se ::mode) ::insert))

(defn normal-mode?
  [se]
  (= (se ::mode) ::normal))

(defn set-insert-mode
  [se]
  (if (insert-mode? se)
    se
    (assoc se ::mode ::insert)))

(defn set-normal-mode
  [se]
  (cond (insert-mode? se)
          (assoc se ::mode ::normal
                    ::col (max 1 (dec (se ::col))))
        (normal-mode? se)
          se))

(defn get-col
  [se]
  (se ::col))

(defn get-row
  [se]
  (se ::row))

(defn get-char
  ([se row col]
   (-> se
       ::lines
       (get (dec row))
       (get (dec col))
       ::char))
  ([se]
   (get-char se (get-row se) (get-col se))))


(comment
  
  (let [se (sub-editor "abcd\nxyz")]
    (-> se
        get-char))

  (let [se (sub-editor "abcd\nxyz")]
    (-> se
        (get-char 2 3))))


(defn get-attribute
  [se attr]
  (-> se
      ::lines
      (get (dec (get-row se)))
      (get (dec (get-col se))) attr))

;(into [] (subvec v 0 n))
(defn insert-line-break
  [se row col]
  (update se ::lines
    (fn [lines]
      (let [l (lines (dec row))
            l1 (into [] (subvec l 0 (dec col)))
            l2 (into [] (subvec l (dec col)))]
        (-> lines
            (assoc (dec row) l1)
            (insert-in-vector row l2))))))

(defn set-char
  [se row col char]
  (-> se
      (append-line-at-end (- row (line-count se)))
      (append-spaces-to-row row (- col (col-count se row)))
      (assoc-in [::lines (dec row) (dec col)] {::char char})))

(defn insert-char
  ([se row col char]
   (if (= char \newline)
     (insert-line-break se row col)
     (update-in se [::lines (dec row)] #(insert-in-vector % (dec col) {::char char}))))
  ([se char]
   (-> se
       (insert-char (get-row se) (get-col se) char)
       (assoc ::col (if (= char \newline) 1 (inc (get-col se)))
              ::row (if (= char \newline) (inc (get-row se)) (get-row se))))))

(comment
  (let [se (sub-editor "abcd\nxyz")]
    (-> se
        ;(insert-char 2 4 \k)
        (insert-char \1)
        (insert-char \2)
        backward-char
        (insert-char \newline)
        (insert-char \l)
        forward-char
        (insert-char \m)
        get-text)))

(defn set-attribute
  [se row col attr value]
  (-> se
      (append-line-at-end (- row (line-count se)))
      (append-spaces-to-row row (- col (col-count se row)))
      (assoc-in [::lines (dec row) (dec col)] {attr value})))

(comment
  
  (let [se (sub-editor "abcd\nxyz")]
    (-> se
        (set-char 5 6 \k)
        get-text)
  ))

(defn forward-char
  ([se n]
   (let [linevec (-> se ::lines (get (dec (get-row se))))
         maxcol (+ (count linevec) (if (insert-mode? se) 1 0))
         newcol (max 1 (min maxcol (+ (se ::col) n)))]
     (assoc se ::col newcol ::mem-col newcol))) 
  ([se]
   (forward-char se 1)))

(comment
  (let [se (sub-editor "abcd\nxyz")]
    (-> se
        forward-char
        get-text)))

(defn backward-char
  ([se n]
   (forward-char se (- n)))
  ([se]
   (backward-char se 1)))

(defn next-line
  ([se n]
   (let [newrow (max 1 (min (count (se ::lines)) (+ (se ::row) n)))
         linevec (-> se ::lines (get (dec newrow)))
         maxcol (+ (count linevec) (if (insert-mode? se) 1 0))
         newcol (max 1 (min maxcol (se ::mem-col)))]
     (assoc se ::row newrow ::col newcol))) 
  ([se]
   (next-line se 1)))

(defn previous-line
  ([se n]
   (next-line se (- n)))
  ([se]
   (previous-line se 1)))

(defn get-text
  [se]
  (str/join "\n" (map (fn [line] (str/join "" (map ::char line))) (se ::lines))))

(comment
  (-> (sub-editor "abcd\nxyz") (forward-char 3) next-line)
  (= (-> (sub-editor "abcd\nxyz") (forward-char 3) next-line get-char) \z)
  (-> (sub-editor "abcd\nxyz") (insert-char 4 5 \k) get-text)

  (get-text (sub-editor "abcd\nxyz"))
)

