(ns liq2.experiments.tty-output
  (:require [liq2.datastructures.sub-editor :as sub-editor]
            [clojure.string :as str]))

(def esc "\033[")
(def cache (atom {}))

(defn- tty-print
  [& args]
  (.print (System/out) (str/join "" args)))

(defn output-handler
  [se frame]
  (doseq [row (range 1 20) col (range 1 80)]
    ;; Check if row has changed...
    (let [c (or (sub-editor/get-char se row col) \space)]
      (when (not= c (@cache [row col]))
        (tty-print esc (+ row 10) ";" (+ col 10) "H" esc "s" (or (sub-editor/get-char se row col) \space))
        (swap! cache assoc [row col] c))))
  (tty-print esc (+ (sub-editor/get-row se) 10) ";" (+ (sub-editor/get-col se) 10) "H" esc "s"))