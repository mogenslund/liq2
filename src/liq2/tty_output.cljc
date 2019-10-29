(ns liq2.tty-output
  (:require [liq2.buffer :as buffer]
            [clojure.string :as str]))

(def esc "\033[")
(def cache (atom {}))

(defn- tty-print
  [& args]
  #?(:clj (.print (System/out) (str/join "" args))
     :cljs (js/process.stdout.write (str/join "" args))))

(defn output-handler
  [buf frame]
  (doseq [row (range 1 20) col (range 1 80)]
    ;; Check if row has changed...
    (let [c (or (buffer/get-char buf row col) \space)]
      (when (not= c (@cache [row col]))
        (tty-print esc (+ row 10) ";" (+ col 10) "H" esc "s" (or (buffer/get-char buf row col) \space))
        (swap! cache assoc [row col] c))))
  (tty-print esc (+ (buffer/get-row buf) 10) ";" (+ (buffer/get-col buf) 10) "H" esc "s"))