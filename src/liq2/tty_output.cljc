(ns liq2.tty-output
  (:require [liq2.buffer :as buffer]
            [clojure.string :as str]))

(def esc "\033[")
(def cache (atom {}))

(defn- tty-print
  [& args]
  #?(:clj (.print (System/out) (str/join "" args))
     :cljs (js/process.stdout.write (str/join "" args))))

(defn calculate-wrapped-row-dist
  [buf cols tow]
  (let [r1 (buffer/get-row buf)
        r2 (tow 0)]
  ))

(defn recalculate-tow
  [buf rows cols tow]
  (let [towmargin (if (> rows 12) 5 0)]
  ))

(defn output-handler
  [buf frame]
  (let [left 0
        top 0]
  (doseq [row (range 1 20) col (range 1 80)]
    ;; Check if row has changed...
    (let [c (or (buffer/get-char buf row col)
                (if (and (= col 1) (> row (buffer/line-count buf))) (str esc "36m~" esc "0m") \space))]
      (when (not= c (@cache [row col]))
        (tty-print esc (+ row top) ";" (+ col left) "H" esc "s" c)
        (swap! cache assoc [row col] c))))
  (tty-print esc (+ (buffer/get-row buf) top) ";" (+ (buffer/get-col buf) left) "H" esc "s")))

