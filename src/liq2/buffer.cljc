(ns liq2.buffer
  (:require [clojure.string :as str]))

;; TODO Use points and regions whereever it makes sense
;; and depricate functions taking row and col as input when
;; actually a point is appropriate.

;; TODO Use regions and actions on regions, like:
;; end-of-word use word-region, change change-word
;; ord delete-word.
;; In addition: change-outer-word, etc.

(defn buffer
  ([text {:keys [name filename top left rows cols major-mode mode] :as options}]
   (let [lines (mapv (fn [l] (mapv #(hash-map ::char %) l)) (str/split text #"\r?\n" -1))]
     {::name (or name "")
      ::filename filename
      ::lines lines
      ::lines-undo ()  ;; Conj lines into this when doing changes
      ::lines-stack (list {::lines lines ::cursor {::row 1 ::col 1}}) ;; To use in connection with undo
      ::line-ending "\n" 
      ::cursor {::row 1 ::col 1}
      ::selection nil
      ::window {::top (or top 1) ::left (or left 1) ::rows (or rows 1) ::cols (or cols 80)}
      ::mem-col 1                ; Remember column when moving up and down
      ::tow {::row 1 ::col 1}    ; Top of window
      ::mode (or mode :normal)
      ::encoding :utf-8          ; This allows cursor to be "after line", like vim. (Separate from major and minor modes!)
      ::search-word ""
      ::dirty false
      ::major-mode (or major-mode :clojure-mode)
      ::minor-modes []}))
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
  (let [newstack (conj (buf ::lines-stack) (select-keys buf [::lines ::cursor]))]
    (assoc buf ::lines-stack newstack
               ::lines-undo newstack)))

(defn undo
  "Returns the first buffer in the undo stack."
  [buf]
  (if (empty? (buf ::lines-undo))
    buf
    (assoc buf ::lines (-> buf ::lines-undo first ::lines)
               ::cursor (-> buf ::lines-undo first ::cursor)
               ::lines-stack (conj (buf ::lines-stack) (-> buf ::lines-undo first))
               ::lines-undo (rest (buf ::lines-undo)))))

(defn debug-clear-undo
  [buf]
  (assoc buf
    ::lines-undo (list)
    ::lines-stack (list)))

;; Information
;; ===========

(defn get-window-top [w] (w ::top))
(defn get-window-left [w] (w ::left))
(defn get-window-rows [w] (w ::rows))
(defn get-window-cols [w] (w ::cols))


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

(defn get-point-col [p] (p ::col))
(defn get-point-row [p] (p ::row))

(defn get-col [buf] (-> buf ::cursor ::col))

(defn get-row [buf] (-> buf ::cursor ::row))

(defn set-point
  ([buf p] (assoc buf ::cursor p))
  ([buf row col] (set-point buf {::row row ::col col})))

(defn get-point
  [buf]
  (buf ::cursor))

(defn update-mem-col
  [buf]
  (assoc buf ::mem-col ((get-point buf) ::col)))

(defn set-selection
  ([buf p] (assoc buf ::selection p))
  ([buf row col] (set-selection buf {::row row ::col col}))
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

(defn set-tow
  [buf p]
  (assoc buf ::tow p))

(defn get-tow
  [buf]
  (buf ::tow))

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

(comment (pr-str (get-line (buffer "aaa\nbbb\nccc") 2)))

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
                true (str/join "" (map ::char (lines n)))))))))
  ([buf r] (get-text buf (first r) (second r))))

(comment
  (get-text (buffer "abcdefg\n1234567\nABCDEF" {}) {::row 1 ::col 1} {::row 2 ::col 3})
  (get-text (buffer "abcdefg\n1234567\nABCDEF" {}) {::row 1 ::col 2} {::row 2 ::col 3})
  (get-text (buffer "abcdefg\n1234567\nABCDEF" {}) {::row 2 ::col 2} {::row 2 ::col 3})
  (get-text (buffer "abcdefg\n\nABCDEF\n\n" {}) {::row 1 ::col 2} {::row 6 ::col 1} )
)

(defn previous-point
  "If only a buffer is supplied the point will be set
  on the buffer, otherwise:
  The previous point will be returned or nil, if the
  input is the first point"  
  ([buf p]
   (cond (> (p ::col) 1) (update-in p [::col] dec)
         (> (p ::row) 1) {::row (dec (p ::row)) ::col (col-count buf (dec (p ::row)))})) 
  ([buf] (if (= (get-point buf) {::row 1 ::col 1})
           buf
           (set-point buf (previous-point buf (get-point buf))))))

(defn next-point
  ([buf p]
   (cond (< (p ::col) (col-count buf (p ::row))) (update-in p [::col] inc)
         (< (p ::row) (line-count buf)) {::row (inc (p ::row)) ::col (min 1 (col-count buf (inc (p ::row))))})) 
  ([buf] (if-let [p (next-point buf (get-point buf))]
           (set-point buf p)
           buf)))

(comment (next-point (buffer "aaa\n\nbbb\nccc") {::row 5 ::col 1}))
(comment (previous-point (buffer "aaa\n\nbbb\nccc") {::row 2 ::col 1}))
(comment (previous-point (buffer "aaa\n\nbbb\nccc") {::row 1 ::col 1}))
(comment
  (let [buf (buffer "aaa\n\nbbb\nccc")]
    (loop [p {::row 4 ::col 3}]
      (when (previous-point buf p)
        (println (previous-point buf p))
        (recur (previous-point buf p))))))

(defn end-point
  [buf]
  {::row (line-count buf) ::col (col-count buf (line-count buf))})

(comment
  (end-point (buffer "aaaa bbbb\nccc")))

(defn start-point
  [buf]
  {::row 1 ::col 1})

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
   ([buf row col] (selected? buf {::row row ::col col})))

;; Movements
;; =========

(defn right
  ([buf n]
   (let [linevec (-> buf ::lines (get (dec (get-row buf))))
         maxcol (+ (count linevec) (if (= (get-mode buf) :insert) 1 0))
         newcol (max 1 (min maxcol (+ (get-col buf) n)))]
     (-> buf
         (set-point {::row (get-row buf) ::col newcol})
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
     (set-point buf {::row newrow ::col newcol}))) 
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
      (set-point {::row (get-row buf) ::col (col-count buf (get-row buf))}) 
      (assoc ::mem-col (col-count buf (get-row buf)))))

(defn beginning-of-line
  [buf]
  (-> buf
      (set-point {::row (get-row buf) ::col 1}) 
      (assoc ::mem-col 1)))

(defn beginning-of-buffer
  [buf]
  (-> buf
      (set-point {::row 1 ::col 1}) 
      (assoc ::mem-col 1)))


(defn end-of-buffer
  [buf]
  (-> buf
      (set-point {::row (line-count buf) ::col (col-count buf (line-count buf))})
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
  (get-char (buffer "abcd\nxyz") {::row 1 ::col 1})

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

(defn get-style
  ([buf row col]
   (-> buf
       ::lines
       (get (dec row))
       (get (dec col))
       ::style))
  ([buf] (get-style buf (get-row buf) (get-col buf))))

(defn set-style
  ([buf row col style]
   (if (and (get-char buf row col) (not= (get-style buf row col) style))
     (assoc-in buf [::lines (dec row) (dec col) ::style] style)
     buf))
  ([buf p style] (set-style buf (p ::row) (p ::col) style))
  ([buf row col1 col2 style]
   (loop [b buf col col1]
     (if (> col col2)
       b
       (recur (set-style b row col style) (inc col))))))

(defn insert-char
  ([buf row col char]
   (if (= char \newline)
     (insert-line-break buf row col)
     (update-in (set-dirty buf true) [::lines (dec row)] #(insert-in-vector % (dec col) {::char char}))))
  ([buf char]
   (-> buf
       (insert-char (get-row buf) (get-col buf) char)
       (set-point {::row (if (= char \newline) (inc (get-row buf)) (get-row buf))
                   ::col (if (= char \newline) 1 (inc (get-col buf)))}))))


(defn append-line
  ([buf row]
   (-> buf
       (set-dirty true)
       (update ::lines #(insert-in-vector % row []))
       (set-point {::row (inc (get-row buf)) ::col 1})
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
             ::cursor {::row 1 ::col 1}
             ::mem-col 1)
     (let [b1 (update buf ::lines #(remove-from-vector % row))
           newrow (min (line-count b1) row)
           newcol (min (col-count b1 newrow) (get-col buf))]
        (set-point b1 newrow newcol))))
  ([buf] (delete-line buf (get-row buf))))

(comment (pr-str (get-text (-> (buffer "aaa\nbbb\nccc") down right delete-line))))
(comment (pr-str (get-text (-> (buffer "aaa\nbbb\nccc") down down delete-line))))

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
      (-> (nth (iterate #(delete-line % (p ::row))
                        (update buf ::lines
                                    #(insert-in-vector % (q ::row) (into [] (concat t1 t2)))))
               (- (q ::row) (p ::row) -1))
          set-normal-mode
          (set-point p))))
  ([buf]
   (if-let [p (get-selection buf)]
     (delete buf (get-point buf) p)
     buf)))


(comment (pr-str (get-text (delete (buffer "aa\naa\naa") {::row 1 ::col 2} {::row 2 ::col 2}))))
(comment (pr-str (get-text (delete (buffer "aa\nbb\ncc") {::row 2 ::col 2} {::row 3 ::col 2}))))
(comment (pr-str (get-text (delete (buffer "aa\nbbccdd\nee") {::row 2 ::col 3} {::row 2 ::col 4}))))
(comment (pr-str (get-text (delete (buffer "aa\nbb\ncc") {::row 1 ::col 2} {::row 3 ::col 2}))))
(comment (pr-str (get-text (delete (buffer "aa\n\nbb\ncc") {::row 1 ::col 1} {::row 2 ::col 1}))))
(comment (pr-str (get-text (delete (buffer "aa\n\nbb\ncc") {::row 2 ::col 1} {::row 4 ::col 2}))))
(comment (pr-str (get-text (delete (buffer "aaaaS\nK\nTbbbb\nbbbb") {::row 1 ::col 1} {::row 2 ::col 0}))))




(defn delete-region
  [buf r]
  (if r
    (delete buf (first r) (second r))
    buf))

(comment (get-text (delete-region (buffer "aaaa") [{::row 1 ::col 2} {::row 1 ::col 4}])))
(comment (pr-str (get-text (delete-region (buffer "aa\naa\naa") [{::row 1 ::col 2} {::row 2 ::col 1}]))))

(defn shrink-region
  [buf r]
  (when r
    (let [p1 (first r)
          p2 (second r)]
      (if (= (point-compare p1 p2) -1)
        [(next-point buf p1) (previous-point buf p2)]
        [(previous-point buf p1) (next-point buf p2)]))))

(defn delete-backward
  [buf]
  (cond (> (get-col buf) 1) (-> buf left delete-char)
        (= (get-row buf) 1) buf
        true (let [v (-> buf ::lines (get (dec (get-row buf))))]
               (-> buf
                   (delete-line (get-row buf))
                   (update-in [::lines (- (get-row buf) 2)] #(into [] (concat % v)))
                   (set-point {::row (dec (get-row buf)) ::col (inc (col-count buf (dec (get-row buf))))})))))

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

(defn delete-to-line-end
  [buf]
  (left (delete-region buf [(get-point buf) {::row (get-row buf) ::col (col-count buf (get-row buf))}])))

(defn clear
  [buf]
  (assoc buf ::lines [[]]
             ::cursor {::row 1 ::col 1} 
             ::mem-col 1))

(defn split-buffer
  ([buf p]
   (if (= p (start-point buf))
     [(clear buf) buf]
     [(delete-region buf [p (end-point buf)])
      (delete-region buf [(start-point buf) {::row (get-point-row p) ::col (dec (get-point-col p))}])]))
  ([buf] (split-buffer buf (get-point buf))))

(comment (map get-text (split-buffer (buffer "aaaaSTbbbb\nbbbb") {::row 1 ::col 6})))
(comment (map get-text (split-buffer (buffer "aaaaS\nK\nTbbbb\nbbbb") {::row 2 ::col 1})))
(comment (map get-text (split-buffer (buffer "aa") {::row 1 ::col 1})))
(comment (map get-text (split-buffer (buffer "aa") {::row 2 ::col 1})))
(comment (map get-text (split-buffer (buffer "aa\n\nccc") {::row 2 ::col 1})))


(defn append-buffer
  [buf buf1]
  (-> buf
      (update-in [::lines (dec (line-count buf))] #(into [] (concat % (first (buf1 ::lines)))))
      (update ::lines #(into [] (concat % (rest (buf1 ::lines)))))
      (set-dirty true)))

(comment (pr-str (get-text (append-buffer (buffer "aaa\nbbb") (buffer "ccc\ndddd")))))
(comment (pr-str (get-text (append-buffer (buffer "aaa") (buffer "bbb\n\n")))))
(comment (pr-str (get-text (buffer "bbb\n\n"))))
(comment (pr-str (get-text (append-buffer (buffer "aaa") (buffer "\nbbb")))))

(defn insert-buffer
  ([buf p buf0]
   (let [[b1 b2] (split-buffer buf p)]
     (append-buffer
       b1
       (append-buffer buf0 b2))))
  ([buf buf0]
   (insert-buffer buf (get-point buf) buf0)))

(comment (pr-str (get-text (insert-buffer (buffer "aaa\n\nbbb") (buffer "")))))
(comment (get-text (insert-buffer (buffer "aaaaabbbbb") {::row 1 ::col 6} (buffer "cccc"))))
(comment (get-text (insert-buffer (buffer "aaaaa\n\nbbbbb") {::row 2 ::col 1} (buffer "cccc"))))
(comment (get-text (insert-buffer (buffer "aaaaabbbbb") (buffer "cccc"))))
(comment (get-row (insert-buffer (buffer "aaaaabbbbb") (buffer "cccc"))))
(comment (pr-str (get-text (insert-buffer (buffer "") (buffer "")))))
(comment (get-text (insert-buffer (buffer "") (buffer ""))))
(comment (pr-str (get-text (insert-buffer (buffer "aaa") (buffer "bbb\n")))))

(defn insert-string
  [buf text]
  (insert-buffer buf (buffer text)))

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

(defn match-before
  [buf p0 re]
  (loop [p (previous-point buf p0)]
    (when p
      (if (re-find re (str (get-char buf p)))
        p
        (recur (previous-point buf p))))))

(comment
  (previous-point (buffer "aaa bbb ccc") {::row 1 ::col 8})
  (previous-point (buffer "aaa bbb ccc") {::row 1 ::col 1})
  (match-before (buffer "aaa bbb ccc") {::row 1 ::col 8} #"a"))


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

;; Regions
;; =======

(defn paren-region
  ([buf p]
   (let [p0 (paren-match-before buf (if (= (get-char buf p) \)) (previous-point buf p) p) \()
         p1 (when p0 (paren-match-after buf (next-point buf p0) \)))]
     (when p1 [p0 p1])))
  ([buf] (paren-region buf (get-point buf))))

(defn bracket-region
  ([buf p]
   (let [p0 (paren-match-before buf (if (= (get-char buf p) \]) (previous-point buf p) p) \[)
         p1 (when p0 (paren-match-after buf (next-point buf p0) \]))]
     (when p1 [p0 p1])))
  ([buf] (bracket-region buf (get-point buf))))

(defn brace-region
  ([buf p]
   (let [p0 (paren-match-before buf (if (= (get-char buf p) \}) (previous-point buf p) p) \{)
         p1 (when p0 (paren-match-after buf (next-point buf p0) \}))]
     (when p1 [p0 p1])))
  ([buf] (brace-region buf (get-point buf))))

(comment (paren-region (buffer "(asdf)")))
(comment (bracket-region (buffer "[asdf]")))

(defn line-region
  ([buf p]
   (when (<= (p ::row) (line-count buf))
     [(assoc p ::col 1) (assoc p ::col (col-count buf (p ::row)))]))
  ([buf] (line-region buf (get-point buf)))) 

(comment (line-region (buffer "abc\ndefhi") {::row 2 ::col 2}))
  

(defn paren-matching-region
  "Forward until first paren on given row.
  Depending on type and direction move to corresponding
  paren.
  Returns nil if there is no hit."
  [buf p]
  (let [pbegin (start-point buf)
        pend (end-point buf)
        ncol (fn [p0] (update p0 ::col inc))
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
              (cond (= nnext 0) [p1 p0]
                    (= p0 start-point) nil
                    (= p0 end-point) nil
                    true (recur (direction buf p0) nnext)))))))))

(comment (paren-region (buffer "ab (cde\naaa bbb (ccc))") {::row 2 ::col 5}))
(comment (paren-region (buffer "ab (cde\naaa bbb (ccc))") {::row 2 ::col 5}))
(comment (pr-str (paren-region (buffer "ab cde\naaa bbb ccc") {::row 2 ::col 3})))

(defn move-matching-paren
  [buf]
  (let [r (paren-matching-region buf (get-point buf))]
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
       (cond (= p {::row 1 ::col 1}) b 
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
       (cond (= p {::row rows ::col cols}) b 
             (and is-word (= (p ::col) cols)) b
             (= (p ::col) cols) (recur (next-point b))
             (and is-word (re-matches #"\W" (str (get-char (right b))))) b
             true (recur (right b))))))
  ([buf n] (nth (iterate end-of-word buf) n)))

(defn word-region
  ([buf]
   (let [b1 (-> buf left end-of-word)]
     [(-> b1 beginning-of-word get-point)
             (-> b1 get-point)]))
  ([buf p]
   (word-region (set-point buf p))))

(comment (word-region (buffer "aaa bbb ccc") {::row 1 ::col 5}))
(comment (word-region (buffer "aaa bbb ccc")))

(defn word-forward
  ([buf]
   (loop [b (or (next-point buf) buf)]
     (let [p (get-point b)
           rows (line-count b)
           cols (col-count b (p ::row))
           c (str (get-char b))
           is-word (re-matches #"\w" c)]
       (cond (= p {::row rows ::col cols}) b 
             (and is-word (= (p ::col) 1)) b
             (and is-word (re-matches #"\W" (str (get-char (left b))))) b
             true (recur (next-point b))))))
  ([buf n] (nth (iterate word-forward buf) n)))

(defn calculate-wrapped-row-dist
  [buf cols row1 row2]
  (reduce #(+ 1 %1 (quot (dec (col-count buf %2)) cols)) 0 (range row1 row2))) 

(defn recalculate-tow
  "This is a first draft, which does not handle edge
  cases with very long lines and positioning logic."
  [buf rows cols tow1]
  (cond (< (get-row buf) (tow1 ::row)) (assoc tow1 ::row (get-row buf))
        (> (- (get-row buf) (tow1 ::row)) rows)
          (recalculate-tow buf rows cols (assoc tow1 ::row (- (get-row buf) rows)))
        (> (calculate-wrapped-row-dist buf cols (tow1 ::row) (+ (get-row buf) 1)) rows)
          (recalculate-tow buf rows cols (update tow1 ::row inc))
        true tow1))

(defn update-tow
  [buf]
  (let [w (buf ::window)]
    (set-tow buf (recalculate-tow buf (w ::rows) (w ::cols) (get-tow buf)))
  ;(set-tow buf {::row 1 ::col 1})
    ))

(comment (update-tow (buffer "ab[[cd]\nx[asdf]yz]")))

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
    (paren-match-before buf {::row 2 ::col 8} \[))

  (let [buf (buffer "aaa bbb ccc")]
    (beginning-of-word (set-point buf {::row 1 ::col 8})))

  (let [buf (buffer "aaa bbb ccc")]
    (match-before buf {::row 1 ::col 8} #"a"))

  (pr-str (get-line (buffer "") 2))
  
  (let [buf (buffer "ab[[cd]\nx[asdf]yz]")]
    (paren-match-before buf {::row 1 ::col 3} \]))

  (let [buf (buffer "ab((cd)\nx(asdf)yz)")]
    (paren-match-before buf {::row 2 ::col 5} \)))

  (let [buf (buffer "ab((cd)\nx(asdf)yz)")]
    (sexp-at-point buf {::row 2 ::col 2}))

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