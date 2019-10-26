(ns liq2.datastructures.sub-editor
  (:require [clojure.string :as str]))

; https://devhints.io/vimscript-functions

(defn sub-editor
  [text]
  {::lines (mapv (fn [l] (mapv #(hash-map ::char %) l)) (str/split-lines text))
   ::encoding :?
   ::line-ending :unix
   ::col 1
   ::row 1
   ::insert false                 ; This allows cursor to be "after line", like vim
   ::major-mode :fundamental-mode
   ::minor-modes []})

(defn get-col
  [se]
  (se ::col))

(defn get-row
  [se]
  (se ::row))

(defn get-current-char
  [se]
  (-> se
      ::lines
      (get (dec (get-col se)))
      (get (dec (get-row se))) ::char))

(defn forward-char
  [se]
  (cond (< (-> se ::lines (se ::col)) count) (se ::col)) (update se ::col inc)
        true se))

(def se (sub-editor "This is a test\nLine 2"))

(-> se get-current-char)
(-> se forward-char get-current-char)
(get-current-char se)

(-> se ::lines (get (dec (get-col se))) (get (dec (get-row se))) ::char)