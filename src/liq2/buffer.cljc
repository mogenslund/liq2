(ns liq2.buffer
  (:require [clojure.string :as str]))

(defn point
  [row col]
  {::row row
   ::col col})

(defn buffer
  ([text {:keys [name filename top left rows cols major-mode mode] :as options}]
   {::name (or name "")
    ::filename filename
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
    ::major-mode (or major-mode :clojure-mode)
    ::minor-modes []})           
  ([text] (buffer text {})))

(defn insert-in-vector
  [v n elem]
  (into [] (concat
             (into [] (subvec v 0 n))
             [elem]
             (into [] (subvec v n)))))

(defn remove-from-vector
  ([v n]
   (if (<= 1 n (count v))
     (into [] (concat
                (into [] (subvec v 0 (dec n)))
                (into [] (subvec v n))))
     v))
  ([v m n]
    (if (<= 1 m n (count v))
     (into [] (concat
                (into [] (subvec v 0 (dec m)))
                (into [] (subvec v n))))
     v)))


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
  ([buf row col] (set-point buf (point row col))))

(defn get-point
  [buf]
  (buf ::cursor))

(defn update-mem-col
  [buf]
  (assoc buf ::mem-col ((get-point) ::col)))

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

(defn set-visual-mode
  [buf]
  (-> buf
    (set-mode :visual)
    set-selection))

(defn set-normal-mode
  [buf]
  (-> buf
      (set-mode :normal)
      remove-selection))
  
(defn set-insert-mode
  [buf]
  (-> buf
      (set-mode :insert)
      remove-selection))

(defn point-compare
  [p1 p2]
  (compare [(p1 ::row) (p1 ::col)]
           [(p2 ::row) (p2 ::col)]))

(defn get-line
  "Get line as string"
  ([buf row]
   (str/join (map ::char (get (buf ::lines) (dec row)))))
  ([buf row col]
   (let [r (get (buf ::lines) (dec row))]
     (if (> col (count r))
       ""
       (str/join (map ::char (subvec r (dec col)))))))
  ([buf] (get-line buf (get-row buf))))

(comment
  (str/join (map ::char (get ((buffer "abcde") :liq2.buffer/lines) 1)))
  (get-line (buffer "abcde") 1)
  (get-line (buffer "abcde") 1 2)
  (type (get-line (buffer "abcde") 2 2))
  (type (get-line (buffer "abcde") 10))
  (get-line (buffer "abcde") 1 10))

(defn get-word
  ([buf row col]
   (loop [l (str/split (get-line buf row) #" ") idx 1]
     (let [w (first l)]
       (if (or (> (+ (count w) idx) col) (empty? l))
       w
       (recur (rest l) (+ idx (count w) 1))))))
  ([buf] (get-word buf (get-row buf) (get-col buf))))

(defn get-text
  ([buf]
   (str/join "\n" (map (fn [line] (str/join "" (map ::char line))) (buf ::lines))))
  ([buf p1 p2]
    (let [p (if (= (point-compare p1 p2) -1) p1 p2)  ; first 
          q (if (= (point-compare p1 p2) -1) p2 p1)  ; second
          lines (buf ::lines)]
    (str/join "\n"
      (filter #(not (nil? %))
        (for [n (range (count lines))]
          (cond (< (inc n) (p ::row)) nil
                (= (inc n) (p ::row) (q ::row)) (str/join "" (map ::char (subvec (lines n) (dec (p ::col)) (min (q ::col) (count (lines n))))))
                (= (inc n) (p ::row)) (str/join "" (map ::char (subvec (lines n) (dec (p ::col)))))
                (= (inc n) (q ::row)) (str/join "" (map ::char (subvec (lines n) 0 (min (q ::col) (count (lines n))))))
                (>= n (q ::row)) nil
                true (str/join "" (map ::char (lines n))))))))))

(comment
  (get-text (buffer "abcdefg\n1234567\nABCDEF" {}) (point 1 1) (point 2 3))
  (get-text (buffer "abcdefg\n1234567\nABCDEF" {}) (point 1 2) (point 2 3))
  (get-text (buffer "abcdefg\n1234567\nABCDEF" {}) (point 2 2) (point 2 3))
  (get-text (buffer "abcdefg\n\nABCDEF\n\n" {}) (point 1 2) (point 6 1))
)

(defn previous-point
  [buf p]
  (cond (> (p ::col) 1) (point (p ::row) (dec (p ::col)))
        (> (p ::row) 1) (point (dec (p ::row)) (col-count buf (dec (p ::row)))))) 

(defn next-point
  [buf p]
  (cond (< (p ::col) (col-count buf (p ::row))) (point (p ::row) (inc (p ::col)))
        (< (p ::row) (line-count buf)) (point (inc (p ::row)) (min 1 (col-count buf (inc (p ::row))))))) 

(comment
  (let [buf (buffer "aaa\n\nbbb\nccc")]
    (loop [p (point 4 3)]
      (when (previous-point buf p)
        (println (previous-point buf p))
        (recur (previous-point buf p))))))


(defn get-selected-text
  [buf]
  (if-let [p (get-selection buf)]
    (get-text buf (get-point buf) p)
    ""))

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
      (assoc ::mem-col (col-count buf (line-count buf)))))

;; Modifications
;; =============

(defn delete-line
  ([buf row]
   (if (<= (line-count buf) 1)
     (assoc buf ::lines [[]]
             ::cursor (point 1 1)
             ::mem-col 1)
     (-> buf
         previous-line
         (update ::lines #(remove-from-vector % row))
         next-line)))
  ([buf] (delete-line buf (get-row buf))))

(comment
  (-> (buffer "aaa\nbbb\nccc") next-line delete-line))

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
  ([buf p]
   (get-char buf (p ::row) (p ::col)))
  ([buf]
   (get-char buf (get-row buf) (get-col buf))))


(comment
  
  (let [buf (buffer "abcd\nxyz")]
    (-> buf
        get-char))

  (let [buf (buffer "abcd\nxyz")]
    (-> buf
        (get-char 2 3))

  (let [buf (buffer "abcd\n\nxyz")]
    (-> buf
        next-line
        get-char))))


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

(defn set-style
  ([buf row col style]
   (assoc-in buf [::lines (dec row) (dec col) ::style] style))
  ([buf row col1 col2 style]
   (loop [b buf col col1]
     (if (> col col2)
       b
       (recur (set-style b row col style) (inc col))))))

(defn get-style
  ([buf row col]
   (-> buf
       ::lines
       (get (dec row))
       (get (dec col))
       ::style))
  ([buf] (get-style buf (get-row buf) (get-col buf))))

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

;; TODO There should be a insert-buffer to insert buffer into buffer
;;      It should be optimized. Right now (buffer text) is much faster than
;;      (-> (buffer "") (insert-string text))
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
   (append-line buf (get-row buf))))

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

(defn insert-at-line-end
  [buf]
  (-> buf
      end-of-line
      (set-mode :insert)
      forward-char))

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

(defn end-of-word
  [buf]
  buf
  )

(defn paren-match-before
  "(abc (def) hi|jk)"
  [buf p0 paren]
  (let [pmatch {\( \) \) \( \{ \} \} \{ \[ \] \] \[}]
    (loop [p p0 stack (list (pmatch paren))]
      (when p
        (let [c (get-char buf p)
              nstack (cond (nil? c) stack
                           (= (pmatch c) (first stack)) (rest stack)
                           (some #{c} (keys pmatch)) (conj stack c)
                           true stack)]
          (if (empty? nstack)
            p
            (recur (previous-point buf p) nstack)))))))

(defn paren-match-after
  "(abc (def) hi|jk)"
  [buf p0 paren]
  (let [pmatch {\( \) \) \( \{ \} \} \{ \[ \] \] \[}]
    (loop [p p0 stack (list (pmatch paren))]
      (when p
        (let [c (get-char buf p)
              nstack (cond (nil? c) stack
                           (= (pmatch c) (first stack)) (rest stack)
                           (some #{c} (keys pmatch)) (conj stack c)
                           true stack)]
          (if (empty? nstack)
            p
            (recur (next-point buf p) nstack)))))))

(defn sexp-at-point
  ([buf p]
   (let [p0 (paren-match-before buf (if (= (get-char buf p) \)) (previous-point buf p) p) \()
         p1 (when p0 (paren-match-after buf (next-point buf p0) \)))]
     (when p1 (get-text buf p0 p1))))
  ([buf] (sexp-at-point buf (get-point buf))))

;; Emacs has two stages:
;; 1. Where comments and strings are highlighted
;;    Define comment start and comment end
;;    Define string delimiter and escape-char
;; 2. Where keywords are highlighted. These are matched by regexes outside strings
;; https://medium.com/@model_train/creating-universal-syntax-highlighters-with-iro-549501698fd2
;; https://github.com/atom/language-clojure/blob/master/grammars/clojure.cson
;; (re-find #"(?<!\\)(\")" "something \"a string\" else")
;; (re-find #"(?<=(\s|\(|\[|\{)):[\w\#\.\-\_\:\+\=\>\<\/\!\?\*]+(?=(\s|\)|\]|\}|\,))" "abc :def hij :ppp ")

;; Not use regex, but functions, which might be regex! str/index-of or count str/split

;(defn apply-syntax-hl
;  [buf hl]
;  ;; TODO Now only highlighting row 1 as experiment - In progress
;  (loop [b buf col 1 keyw :plain]
;    (if (> col (col-count b 1))
;      b
;      (let [c (get-char b 1 col)
;            keywn (cond (and (= keyw :plain) (= c "\"")) :string
;                        (and (= keyw :plain) (= c ";")) :comment 
;                        (and (= keyw :string) (= c "\"")) :
;  buf))

(comment

  (let [buf (buffer "ab[[cd]\nx[asdf]yz]")]
    (paren-match-before buf (point 2 8) \[))
  
  (let [buf (buffer "ab[[cd]\nx[asdf]yz]")]
    (paren-match-before buf (point 1 3) \]))

  (let [buf (buffer "ab((cd)\nx(asdf)yz)")]
    (paren-match-before buf (point 2 5) \)))

  (let [buf (buffer "ab((cd)\nx(asdf)yz)")]
    (sexp-at-point buf (point 2 2)))

  (let [buf (buffer "abcd\nxyz")]
    (-> buf
        (set-char 5 6 \k)
        get-text)))


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



