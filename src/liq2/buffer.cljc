(ns liq2.buffer
  (:require [clojure.string :as str]))

(defn point
  [row col]
  {::row row
   ::col col})

(defn buffer
  [text {:keys [name top left rows cols major-mode mode] :as options}]
  {::name name
   ::filename nil
   ::lines (mapv (fn [l] (mapv #(hash-map ::char %) l)) (str/split-lines text))
   ::line-ending "\n" 
   ::cursor (point 1 1)
   ::selection nil
   ::top top
   ::left left
   ::rows rows
   ::cols cols
   ::mem-col 1                ; Remember column when moving up and down
   ::mode (or mode :normal)
   ::encoding :utf-8          ; This allows cursor to be "after line", like vim. (Separate from major and minor modes!)
   ::major-mode (or major-mode :fundamental-mode)
   ::minor-modes []})           

(defn insert-in-vector
  [v n elem]
  (into [] (concat
             (into [] (subvec v 0 n))
             [elem]
             (into [] (subvec v n)))))

(defn remove-from-vector
  [v n]
  (if (<= 1 n (count v))
    (into [] (concat
               (into [] (subvec v 0 (dec n)))
               (into [] (subvec v n))))
    v))

;; Information
;; ===========

(defn get-name [buf] (buf ::name))

(defn set-filename [buf path] (assoc buf ::filename path))
(defn get-filename [buf] (buf ::filename))

(defn line-count [buf] (count (buf ::lines)))

(defn col-count [buf row] (-> buf ::lines (get (dec row)) count))

(defn set-mode [buf m] (assoc buf ::mode m))
(defn get-mode [buf] (buf ::mode))


(defn set-major-mode [buf m] (assoc buf ::major-mode m))

(defn get-major-mode [buf] (buf ::major-mode))

(comment 
  (let [buf (buffer "abcd\nxyz")]
    (-> buf
        (set-mode :insert)
        get-mode)))

(defn get-col [buf] (-> buf ::cursor ::col))

(defn get-row [buf] (-> buf ::cursor ::row))

(defn get-text
  [buf]
  (str/join "\n" (map (fn [line] (str/join "" (map ::char line))) (buf ::lines))))

(defn set-top [buf n] (assoc buf ::top n))
(defn get-top [buf] (buf ::top))

(defn set-left [buf n] (assoc buf ::left n))
(defn get-left [buf] (buf ::left))

(defn set-rows [buf n] (assoc buf ::rows n))
(defn get-rows [buf] (buf ::rows))

(defn set-cols [buf n] (assoc buf ::cols n))
(defn get-cols [buf] (buf ::cols))

(defn set-point
  ([buf p] (assoc buf ::cursor p))
  ([buf row col] (set-point (point row col))))

(defn get-point
  [buf]
  (buf ::cursor))

(defn set-selection
  ([buf p] (assoc buf ::selection p))
  ([buf row col] (set-selection buf (point row col)))
  ([buf] (set-selection buf (get-point buf))))

(defn get-selection
  [buf]
  (buf ::selection))

(defn remove-selection
  [buf]
  (assoc buf ::selection nil))
  

(defn point-compare
  [p1 p2]
  (compare [(p1 ::row) (p1 ::col)]
           [(p2 ::row) (p2 ::col)]))

(defn selected?
  ([buf p]
   (let [s (get-selection buf)
         c (get-point buf)]
     (cond (nil? s) false
           (and (<= (point-compare s p) 0) (<= (point-compare p c) 0)) true
           (and (<= (point-compare c p) 0) (<= (point-compare p s) 0)) true
           true false)))
   ([buf row col] (selected? buf (point row col))))

;; Movements
;; =========

(defn forward-char
  ([buf n]
   (let [linevec (-> buf ::lines (get (dec (get-row buf))))
         maxcol (+ (count linevec) (if (= (get-mode buf) :insert) 1 0))
         newcol (max 1 (min maxcol (+ (get-col buf) n)))]
     (-> buf
         (set-point (point (get-row buf) newcol))
         (assoc ::mem-col newcol)))) 
  ([buf]
   (forward-char buf 1)))

(defn backward-char
  ([buf n]
   (forward-char buf (- n)))
  ([buf]
   (backward-char buf 1)))

(defn next-line
  ([buf n]
   (let [newrow (max 1 (min (count (buf ::lines)) (+ (get-row buf) n)))
         linevec (-> buf ::lines (get (dec newrow)))
         maxcol (+ (count linevec) (if (= (get-mode buf) :insert) 1 0))
         newcol (max 1 (min maxcol (buf ::mem-col)))]
     (set-point buf (point newrow newcol)))) 
  ([buf]
   (next-line buf 1)))

(defn previous-line
  ([buf n]
   (next-line buf (- n)))
  ([buf]
   (previous-line buf 1)))

(defn end-of-line
  [buf]
  (-> buf
      (set-point (point (get-row buf) (col-count buf (get-row buf)))) 
      (assoc ::mem-col (col-count buf (get-row buf)))))

(defn beginning-of-line
  [buf]
  (-> buf
      (set-point (point (get-row buf) 1)) 
      (assoc ::mem-col 1)))

(defn beginning-of-buffer
  [buf]
  (-> buf
      (set-point (point 1 1)) 
      (assoc ::mem-col 1)))


(defn end-of-buffer
  [buf]
  (-> buf
      (set-point (point (line-count buf) (col-count buf (line-count buf))))
      (assoc ::mem-col 1)))

;; Modifications
;; =============

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
       (set-point (point (if (= char \newline) (inc (get-row buf)) (get-row buf))
                         (if (= char \newline) 1 (inc (get-col buf))))))))

(defn insert-string
  [buf text]
  (reduce insert-char buf text))

(defn append-line
  ([buf row]
   (-> buf
       (update ::lines #(insert-in-vector % row []))
       (set-point (point (inc (get-row buf)) 1))
       (set-mode :insert)))
  ([buf]
   (append-line buf (buf ::row))))

(defn delete-char
  ([buf row col]
   (update-in buf [::lines (dec row)] #(remove-from-vector % col)))
  ([buf]
   (-> buf
       (delete-char (get-row buf) (get-col buf)))))


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

(defn clear
  [buf]
  (assoc buf ::lines [[]]
             ::cursor (point 1 1)
             ::mem-col 1))

(comment
  
  (let [buf (buffer "abcd\nxyz")]
    (-> buf
        (set-char 5 6 \k)
        get-text)
  ))


(comment
  (let [buf (buffer "abcd\nxyz")]
    (-> buf
        forward-char
        get-text)))


(comment
  (-> (buffer "abcd\nxyz") (forward-char 3) next-line)
  (= (-> (buffer "abcd\nxyz") (forward-char 3) next-line get-char) \z)
  (-> (buffer "abcd\nxyz") (insert-char 4 5 \k) get-text)
  (-> (buffer "abcd\nxyz") append-line get-text)

  (get-text (buffer "abcd\nxyz"))
)



