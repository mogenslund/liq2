(ns liq2.buffer
  (:require [clojure.string :as str]))

;; TODO Use points and regions whereever it makes sense
;; and depricate functions taking row and col as input when
;; actually a point is appropriate.

;; TODO Use regions and actions on regions, like:
;; end-of-word use word-region, change change-word
;; ord delete-word.
;; In addition: change-outer-word, etc.

(defn point
  [row col]
  {::row row
   ::col col})

(defn region
  [p1 p2]
  [p1 p2])

(defn buffer
  ([text {:keys [name filename top left rows cols major-mode mode] :as options}]
   {::name (or name "")
    ::filename filename
    ::lines (mapv (fn [l] (mapv #(hash-map ::char %) l)) (str/split-lines text))
    ::lines-undo ()  ;; Conj lines into this when doing changes
    ::lines-stack () ;; To use in connection with undo
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
    ::search-word ""
    ::dirty false
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

(defn set-undo-point
  "Return new lines with the current lines to the undo stack."
  [buf]
  (let [newstack (conj (buf ::lines-stack) (buf ::lines))]
    (assoc buf ::lines-stack newstack
               ::lines-undo newstack)))

(defn undo
  "Returns the first buffer in the undo stack."
  [buf]
  (if (empty? (buf ::lines-undo))
    buf
    (assoc buf ::lines (-> buf ::lines-undo first)
               ::lines-stack (conj (buf ::lines-stack) (-> buf ::lines-undo first))
               ::lines-undo (rest (buf ::lines-undo)))))

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
  (assoc buf ::mem-col ((get-point buf) ::col)))

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

(defn set-dirty
  [buf val]
  (set-undo-point
    (if (get-filename buf)
      (assoc buf ::dirty val)
      buf)))

(defn dirty?
  [buf]
  (buf ::dirty))

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
  "If only a buffer is supplied the point will be set
  on the buffer, otherwise:
  The previous point will be returned or nil, if the
  input is the first point"  
  ([buf p]
   (cond (> (p ::col) 1) (point (p ::row) (dec (p ::col)))
         (> (p ::row) 1) (point (dec (p ::row)) (col-count buf (dec (p ::row)))))) 
  ([buf] (if (= (get-point buf) (point 1 1))
           buf
           (set-point buf (previous-point buf (get-point buf))))))

(defn next-point
  ([buf p]
   (cond (< (p ::col) (col-count buf (p ::row))) (point (p ::row) (inc (p ::col)))
         (< (p ::row) (line-count buf)) (point (inc (p ::row)) (min 1 (col-count buf (inc (p ::row))))))) 
  ([buf] (if-let [p (next-point buf (get-point buf))]
           (set-point buf p)
           buf)))

(comment (next-point (buffer "aaa\n\nbbb\nccc") (point 5 1)))
(comment (previous-point (buffer "aaa\n\nbbb\nccc") (point 2 1)))
(comment (previous-point (buffer "aaa\n\nbbb\nccc") (point 1 1)))
(comment
  (let [buf (buffer "aaa\n\nbbb\nccc")]
    (loop [p (point 4 3)]
      (when (previous-point buf p)
        (println (previous-point buf p))
        (recur (previous-point buf p))))))

(defn end-point
  [buf]
  (point (line-count buf) (col-count buf (line-count buf))))

(comment
  (end-point (buffer "aaaa bbbb\nccc")))

(defn start-point
  [buf]
  (point 1 1))

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

(defn right
  ([buf n]
   (let [linevec (-> buf ::lines (get (dec (get-row buf))))
         maxcol (+ (count linevec) (if (= (get-mode buf) :insert) 1 0))
         newcol (max 1 (min maxcol (+ (get-col buf) n)))]
     (-> buf
         (set-point (point (get-row buf) newcol))
         (assoc ::mem-col newcol)))) 
  ([buf]
   (right buf 1)))

(defn left
  ([buf n]
   (right buf (- n)))
  ([buf]
   (left buf 1)))

(defn down
  ([buf n]
   (let [newrow (max 1 (min (count (buf ::lines)) (+ (get-row buf) n)))
         linevec (-> buf ::lines (get (dec newrow)))
         maxcol (+ (count linevec) (if (= (get-mode buf) :insert) 1 0))
         newcol (max 1 (min maxcol (buf ::mem-col)))]
     (set-point buf (point newrow newcol)))) 
  ([buf]
   (down buf 1)))

(defn up
  ([buf n]
   (down buf (- n)))
  ([buf]
   (up buf 1)))

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


(defn append-line-at-end
  "Append empty lines at end"
  ([buf n]
   (loop [buf0 buf n0 n]
     (if (<= n0 0)
       (set-dirty buf0 true)
       (recur (update buf0 ::lines conj []) (dec n0)))))
  ([buf] (append-line-at-end buf 1)))

(defn append-spaces-to-row
  [buf row n]
  (update-in (set-dirty buf true) [::lines (dec row)] #(into [] (concat % (repeat n {::char \space}))))) 

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
  
  (get-char (buffer "abcd\nxyz"))
  (get-char (buffer "abcd\nxyz") (point 1 ))

  (let [buf (buffer "abcd\nxyz")]
    (-> buf
        (get-char 2 3))

  (let [buf (buffer "abcd\n\nxyz")]
    (-> buf
        down
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
  (update (set-dirty buf true) ::lines
    (fn [lines]
      (let [l (lines (dec row))
            l1 (into [] (subvec l 0 (dec col)))
            l2 (into [] (subvec l (dec col)))]
        (-> lines
            (assoc (dec row) l1)
            (insert-in-vector row l2))))))

(defn set-char
  ([buf row col char]
   (-> buf
       (set-dirty true)
       (append-line-at-end (- row (line-count buf)))
       (append-spaces-to-row row (- col (col-count buf row)))
       (assoc-in [::lines (dec row) (dec col)] {::char char})))
  ([buf char] (set-char buf (get-row buf) (get-col buf) char)))

(defn set-style
  ([buf row col style]
   (if (get-char buf row col)
     (assoc-in buf [::lines (dec row) (dec col) ::style] style)
     buf))
  ([buf p style] (set-style buf (p ::row) (p ::col) style))
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
     (update-in (set-dirty buf true) [::lines (dec row)] #(insert-in-vector % (dec col) {::char char}))))
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
       (set-dirty true)
       (update ::lines #(insert-in-vector % row []))
       (set-point (point (inc (get-row buf)) 1))
       (set-mode :insert)))
  ([buf]
   (append-line buf (get-row buf))))

(defn delete-char
  ([buf row col n]
   (update-in (set-dirty buf true) [::lines (dec row)] #(remove-from-vector % col (+ col n -1))))
  ([buf n]
   (-> buf
       (delete-char (get-row buf) (get-col buf) n)))
  ([buf]
   (-> buf
       (delete-char (get-row buf) (get-col buf) 1))))

(defn delete-line
  ([buf row]
   (if (<= (line-count buf) 1)
     (assoc buf ::lines [[]]
             ::cursor (point 1 1)
             ::mem-col 1)
     (let [b1 (update buf ::lines #(remove-from-vector % row))
           newrow (min (line-count b1) row)
           newcol (min (col-count b1 newrow) (get-col buf))]
        (set-point b1 newrow newcol))))
  ([buf] (delete-line buf (get-row buf))))

(comment (-> (buffer "aaa\nbbb\nccc") down delete-line))


(defn delete
  ([buf p1 p2]
    (let [p (if (= (point-compare p1 p2) -1) p1 p2)  ; first 
          q (if (= (point-compare p1 p2) -1) p2 p1)  ; second
          t1 (if (> (p ::col) 1)
               (subvec (-> buf ::lines (get (dec (p ::row)))) 0 (dec (p ::col)))
               [])  
          t2 (if (< (q ::col) (col-count buf (q ::row)))
               (subvec (-> buf ::lines (get (dec (q ::row)))) (q ::col) (col-count buf (q ::row)))
               [])]  
      (if (= (p ::row) (q ::row))
        (-> buf
            (update-in [::lines (dec (p ::row))] #(remove-from-vector % (p ::col) (q ::col)))
            set-normal-mode
            (set-point p))
        (-> (nth (iterate #(delete-line % (p ::row)) buf) (- (inc (q ::row)) (p ::row)))
            (update ::lines #(insert-in-vector % (dec (p ::row)) (into [] (concat t1 t2))))
            set-normal-mode
            (set-point p)))))
  ([buf]
   (if-let [p (get-selection buf)]
     (delete buf (get-point buf) p)
     buf)))

(defn delete-backward
  [buf]
  (cond (> (get-col buf) 1) (-> buf left delete-char)
        (= (get-row buf) 1) buf
        true (let [v (-> buf ::lines (get (dec (get-row buf))))]
               (-> buf
                   previous-point
                   (delete-line (get-row buf))
                   (update-in [::lines (- (get-row buf) 2)] #(into [] (concat % v)))
                   right))))

(comment
  (let [buf (buffer "abcd\nxyz")]
    (-> buf
        ;(insert-char 2 4 \k)
        (insert-char \1)
        (insert-char \2)
        left
        (insert-char \newline)
        (insert-char \l)
        right
        (insert-char \m)
        get-text)))

(defn insert-at-line-end
  [buf]
  (-> buf
      end-of-line
      (set-mode :insert)
      right))

(defn set-attribute
  [buf row col attr value]
  (if (get-char buf row col)
    (assoc-in buf [::lines (dec row) (dec col)] {attr value})
    buf))

(defn clear
  [buf]
  (assoc buf ::lines [[]]
             ::cursor (point 1 1)
             ::mem-col 1))

(defn match-before
  [buf p0 re]
  (loop [p (previous-point buf p0)]
    (when p
      (if (re-find re (str (get-char buf p)))
        p
        (recur (previous-point buf p))))))

(comment
  (previous-point (buffer "aaa bbb ccc") (point 1 8))
  (previous-point (buffer "aaa bbb ccc") (point 1 1))
  (match-before (buffer "aaa bbb ccc") (point 1 8) #"a"))


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

(defn line-region
  [buf p]
  (when (<= (p ::row) (line-count buf))
    (region (point (p ::row) 1) (point (p ::row) (col-count buf (p ::row))))))

(comment (line-region (buffer "abc\ndefhi") (point 2 2)))
  

(defn paren-region
  "Forward until first paren on given row.
  Depending on type and direction move to corresponding
  paren.
  Returns nil if there is no hit."
  [buf p]
  (let [pbegin (start-point buf)
        pend (end-point buf)
        ncol (fn [p0] (point (p0 ::row) (inc (p0 ::col))))
        pmatch {\( \) \) \( \{ \} \} \{ \[ \] \] \[}
        p1 (loop [p0 p]
             (cond (nil? (get-char buf p0)) nil 
                   (pmatch (get-char buf p0)) p0
                   true (recur (ncol p0))))]
    (when p1
      (let [par1 (get-char buf p1)
            par2 (pmatch par1)
            direction (if (#{\( \[ \{} par1) next-point previous-point)]
        (loop [p0 (direction buf p1) n 1]
          (when p0
            (let [c (get-char buf p0)
                  nnext (cond (= c par1) (inc n)
                              (= c par2) (dec n)
                              true n)]
              (cond (= nnext 0) (region p1 p0)
                    (= p0 start-point) nil
                    (= p0 end-point) nil
                    true (recur (direction buf p0) nnext)))))))))

(comment (paren-region (buffer "ab (cde\naaa bbb (ccc))") (point 2 5)))
(comment (paren-region (buffer "ab (cde\naaa bbb (ccc))") (point 2 5)))
(comment (pr-str (paren-region (buffer "ab cde\naaa bbb ccc") (point 2 3))))

(defn move-matching-paren
  [buf]
  (let [r (paren-region buf (get-point buf))]
    (if r
      (update-mem-col (set-point buf (second r)))
      buf)))



(defn search
  ""
  ([buf w]
   (let [b (assoc buf ::search-word w)
         regex (re-pattern w)
         l (get-line b)
         s (subs l (min (get-col b) (count l)))
         res (str/split s regex 2)]
     (if (>= (count res) 2)
       (right b (inc (count (first res))))
       (loop [row (inc (get-row b))]
         (let [s (get-line b row)]
           (cond (re-find regex s) (set-point b row (inc (count (first (str/split s regex 2)))))
                 (>= row (line-count b)) b
                 true (recur (inc row))))))))
  ([buf] (search buf (buf ::search-word))))

(comment (search (buffer "aaaa bbbb") "b"))

 

(defn sexp-at-point
  ([buf p]
   (let [p0 (paren-match-before buf (if (= (get-char buf p) \)) (previous-point buf p) p) \()
         p1 (when p0 (paren-match-after buf (next-point buf p0) \)))]
     (when p1 (get-text buf p0 p1))))
  ([buf] (sexp-at-point buf (get-point buf))))

(defn word-beginnings
  "TODO Not used"
  [text]
  (reduce
    #(conj %1 (+ (last %1) %2))
    [0]
    (map count (drop-last (str/split text #"(?<=\W)\b")))))

(defn beginning-of-word
  ([buf]
   (loop [b (or (previous-point buf) buf)]
     (let [p (get-point b)
           c (str (get-char b))
           is-word (re-matches #"\w" c)]
       (cond (= p (point 1 1)) b 
             (and is-word (= (p ::col) 1)) b
             (= (p ::col) 1) (recur (previous-point b))
             (and is-word (re-matches #"\W" (str (get-char (left b))))) b
             true (recur (left b))))))
  ([buf n] (nth (iterate beginning-of-word buf) n)))


(defn end-of-word
  ([buf]
   (loop [b (or (next-point buf) buf)]
     (let [p (get-point b)
           rows (line-count b)
           cols (col-count b (p ::row))
           c (str (get-char b))
           is-word (re-matches #"\w" c)]
       (cond (= p (point rows cols)) b 
             (and is-word (= (p ::col) cols)) b
             (= (p ::col) cols) (recur (next-point b))
             (and is-word (re-matches #"\W" (str (get-char (right b))))) b
             true (recur (right b))))))
  ([buf n] (nth (iterate end-of-word buf) n)))

(defn word-forward
  ([buf]
   (loop [b (or (next-point buf) buf)]
     (let [p (get-point b)
           rows (line-count b)
           cols (col-count b (p ::row))
           c (str (get-char b))
           is-word (re-matches #"\w" c)]
       (cond (= p (point rows cols)) b 
             (and is-word (= (p ::col) 1)) b
             (and is-word (re-matches #"\W" (str (get-char (left b))))) b
             true (recur (next-point b))))))
  ([buf n] (nth (iterate word-forward buf) n)))

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

  (let [buf (buffer "aaa bbb ccc")]
    (beginning-of-word (set-point buf (point 1 8))))

  (let [buf (buffer "aaa bbb ccc")]
    (match-before buf (point 1 8) #"a"))

  (pr-str (get-line (buffer "") 2))
  
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
        right
        get-text)))


(comment
  (-> (buffer "") set-insert-mode (get-line 1) pr-str)
  (-> (buffer "") set-insert-mode (insert-char \a) set-normal-mode (get-line 1) pr-str)
  (-> (buffer "") set-insert-mode (insert-char \a) set-normal-mode get-point)
  (-> (buffer "") set-insert-mode (insert-char \a) set-normal-mode left right get-point)
  (-> (buffer "") set-insert-mode (insert-char \a) set-normal-mode left get-point)
  (-> (buffer "abcd\nxyz") (right 3) down)
  (= (-> (buffer "abcd\nxyz") (right 3) down get-char) \z)
  (-> (buffer "abcd\nxyz") (insert-char 4 5 \k) get-text)
  (-> (buffer "abcd\nxyz") append-line get-text)

  (get-text (buffer "abcd\nxyz"))
  (end-of-line (buffer ""))
  (beginning-of-buffer (buffer ""))
  (insert-char (buffer "") \a)
  (insert-char (buffer "") "a")
)