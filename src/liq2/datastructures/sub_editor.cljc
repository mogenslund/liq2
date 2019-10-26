(ns liq2.datastructures.sub-editor
  (:require [clojure.string :as str]))

; https://devhints.io/vimscript-functions

(defn sub-editor
  [text]
  {::lines (mapv (fn [l] (mapv #(hash-map ::char %) l)) (str/split-lines text))
   ::line-ending :unix
   ::col 1
   ::row 1
   ::mem-col 1                ; Remember column when moving up and down
   ::mode ::normal})          ; This allows cursor to be "after line", like vim. (Separate from major and minor modes!)

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
  [se]
  (-> se
      ::lines
      (get (dec (get-row se)))
      (get (dec (get-col se))) ::char))

(defn forward-char
  ([se n]
   (let [linevec (-> se ::lines (get (dec (get-row se))))
         maxcol (+ (count linevec) (if (insert-mode? se) 1 0))
         newcol (max 1 (min maxcol (+ (se ::col) n)))]
     (assoc se ::col newcol ::mem-col newcol))) 
  ([se]
   (forward-char 1)))

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

(comment
  (-> (sub-editor "abcd\nxyz") (forward-char 3) next-line)
  (= (-> (sub-editor "abcd\nxyz") (forward-char 3) next-line get-char) \z)
)
