(ns liq2.buffer
  (:require [clojure.string :as str]))

(defn buffer
  [text]
  {::lines (mapv (fn [l] (mapv #(hash-map ::char %) l)) (str/split-lines text))
   ::line-ending :unix
   ::col 1
   ::row 1
   ::mem-col 1                ; Remember column when moving up and down
   ::mode :normal
   ::encoding :?              ; This allows cursor to be "after line", like vim. (Separate from major and minor modes!)
   ::major-mode :fundamental-mode
   ::minor-modes []})           

(defn insert-in-vector
  [v n elem]
  (into [] (concat
             (into [] (subvec v 0 n))
             [elem]
             (into [] (subvec v n)))))

(defn append-line-at-end
  "Append empty lines at end"
  ([buf n]
   (loop [buf0 buf n0 n]
     (if (<= n0 0)
       buf0
       (recur (update buf0 ::lines conj []) (dec n0)))))
  ([buf] (append-line-at-end buf 1)))

(defn append-spaces-to-row
  [buf row n]
  (update-in buf [::lines (dec row)] #(into [] (concat % (repeat n {::char \space}))))) 

(comment
  (let [buf (buffer "abcd\nxyz")
        row 4
        spaces 5]
    (append-spaces-to-row buf 2 10)
  ))


(defn line-count
  [buf]
  (count (buf ::lines)))

(defn col-count
  [buf row]
  (-> buf ::lines (get (dec row)) count))

(defn get-mode
  [buf]
  (buf ::mode))

(defn set-mode
  [buf m]
  (assoc buf ::mode m))

(comment 
  (let [buf (buffer "abcd\nxyz")]
    (-> buf
        (set-mode :insert)
        get-mode)))

(defn get-col
  [buf]
  (buf ::col))

(defn get-row
  [buf]
  (buf ::row))

(defn get-char
  ([buf row col]
   (-> buf
       ::lines
       (get (dec row))
       (get (dec col))
       ::char))
  ([buf]
   (get-char buf (get-row buf) (get-col buf))))


(comment
  
  (let [buf (buffer "abcd\nxyz")]
    (-> buf
        get-char))

  (let [buf (buffer "abcd\nxyz")]
    (-> buf
        (get-char 2 3))))


(defn get-attribute
  [buf attr]
  (-> buf
      ::lines
      (get (dec (get-row buf)))
      (get (dec (get-col buf))) attr))

;(into [] (subvec v 0 n))
(defn insert-line-break
  [buf row col]
  (update buf ::lines
    (fn [lines]
      (let [l (lines (dec row))
            l1 (into [] (subvec l 0 (dec col)))
            l2 (into [] (subvec l (dec col)))]
        (-> lines
            (assoc (dec row) l1)
            (insert-in-vector row l2))))))

(defn set-char
  [buf row col char]
  (-> buf
      (append-line-at-end (- row (line-count buf)))
      (append-spaces-to-row row (- col (col-count buf row)))
      (assoc-in [::lines (dec row) (dec col)] {::char char})))

(defn insert-char
  ([buf row col char]
   (if (= char \newline)
     (insert-line-break buf row col)
     (update-in buf [::lines (dec row)] #(insert-in-vector % (dec col) {::char char}))))
  ([buf char]
   (-> buf
       (insert-char (get-row buf) (get-col buf) char)
       (assoc ::col (if (= char \newline) 1 (inc (get-col buf)))
              ::row (if (= char \newline) (inc (get-row buf)) (get-row buf))))))

(comment
  (let [buf (buffer "abcd\nxyz")]
    (-> buf
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
  [buf row col attr value]
  (-> buf
      (append-line-at-end (- row (line-count buf)))
      (append-spaces-to-row row (- col (col-count buf row)))
      (assoc-in [::lines (dec row) (dec col)] {attr value})))

(comment
  
  (let [buf (buffer "abcd\nxyz")]
    (-> buf
        (set-char 5 6 \k)
        get-text)
  ))

(defn forward-char
  ([buf n]
   (let [linevec (-> buf ::lines (get (dec (get-row buf))))
         maxcol (+ (count linevec) (if (= (get-mode buf) :insert) 1 0))
         newcol (max 1 (min maxcol (+ (buf ::col) n)))]
     (assoc buf ::col newcol ::mem-col newcol))) 
  ([buf]
   (forward-char buf 1)))

(comment
  (let [buf (buffer "abcd\nxyz")]
    (-> buf
        forward-char
        get-text)))

(defn backward-char
  ([buf n]
   (forward-char buf (- n)))
  ([buf]
   (backward-char buf 1)))

(defn next-line
  ([buf n]
   (let [newrow (max 1 (min (count (buf ::lines)) (+ (buf ::row) n)))
         linevec (-> buf ::lines (get (dec newrow)))
         maxcol (+ (count linevec) (if (= (get-mode buf) :insert) 1 0))
         newcol (max 1 (min maxcol (buf ::mem-col)))]
     (assoc buf ::row newrow ::col newcol))) 
  ([buf]
   (next-line buf 1)))

(defn previous-line
  ([buf n]
   (next-line buf (- n)))
  ([buf]
   (previous-line buf 1)))

(defn end-of-line
  [buf]
  (assoc buf ::col (col-count buf (buf ::row))
            ::mem-col (col-count buf (buf ::row))))

(defn beginning-of-line
  [buf]
  (assoc buf ::col 1 ::mem-col 1))

(defn get-text
  [buf]
  (str/join "\n" (map (fn [line] (str/join "" (map ::char line))) (buf ::lines))))

(comment
  (-> (buffer "abcd\nxyz") (forward-char 3) next-line)
  (= (-> (buffer "abcd\nxyz") (forward-char 3) next-line get-char) \z)
  (-> (buffer "abcd\nxyz") (insert-char 4 5 \k) get-text)

  (get-text (buffer "abcd\nxyz"))
)



