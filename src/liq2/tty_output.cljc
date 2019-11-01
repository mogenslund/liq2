(ns liq2.tty-output
  (:require [liq2.buffer :as buffer]
            [liq2.frame :as frame]
            [clojure.string :as str]))

(def cache (atom {}))
(def tow-cache (atom {})) ; Top of window stored here, for each frame
(def esc "\033[")

(defn- tty-print
  [& args]
  #?(:clj (.print (System/out) (str/join "" args))
     :cljs (js/process.stdout.write (str/join "" args))))

(defn calculate-wrapped-row-dist
  [buf cols row1 row2]
  (reduce #(+ 1 %1 (quot (dec (buffer/col-count buf %2)) cols)) 0 (range row1 row2))) 

(defn recalculate-tow
  "This is a first draft, which does not handle edge
  cases with very long lines and positioning logic."
  [buf rows cols tow1]
  (cond (< (buffer/get-row buf) (tow1 :row)) (assoc tow1 :row (buffer/get-row buf))
        (> (calculate-wrapped-row-dist buf cols (tow1 :row) (+ (buffer/get-row buf) 1)) rows)
          (recalculate-tow buf rows cols (update tow1 :row inc)) ; Moving tow one line at the time (Not optimized!!!)
        true tow1))

(def tow (atom {:row 1 :col 1})) ; TMP: Until frame is implemented

(comment
  (calculate-wrapped-row-dist (buffer/buffer "aaaaaaaaaaaaaaaaaaaaaaaaaa\nbbbbbbbbbbbbbb") 10 2 3)
  )


; Two pointers trow tcol in terminal row col in buffer

(defn output-handler
  [buf fr]
  (let [left (frame/get-left fr)
        top (frame/get-top fr)
        rows (frame/get-rows fr)
        cols (frame/get-cols fr)
        tow (recalculate-tow buf rows cols (or (@tow-cache fr) {:row 1 :col 1}))]
  (swap! tow-cache assoc fr tow)
  (loop [trow (+ top 1) tcol (+ left 1) row (tow :row) col (tow :col) cursor-row nil cursor-col nil]
    (if (<= trow (+ rows top))
      (do
      ;; Check if row has changed...
        (let [c (or (buffer/get-char buf row col)
                    (if (and (= col 1) (> row (buffer/line-count buf))) (str esc "36m~" esc "0m") \space))
              new-cursor-row (if (and (= row (buffer/get-row buf)) (= col (buffer/get-col buf))) trow cursor-row)
              new-cursor-col (if (and (= row (buffer/get-row buf)) (= col (buffer/get-col buf))) tcol cursor-col)]
          (when (not= c (@cache [trow tcol]))
            (tty-print esc trow ";" tcol "H" esc "s" c)
            (swap! cache assoc [trow tcol] c))
          (if (> tcol cols)
            (if (> (buffer/col-count buf row) col) 
              (recur (inc trow) (+ left 1) row (inc col) new-cursor-row new-cursor-col) 
              (recur (inc trow) (+ left 1) (inc row) 1 new-cursor-row new-cursor-col))
            (recur trow (inc tcol) row (inc col) new-cursor-row new-cursor-col))))
      (tty-print esc cursor-row ";" cursor-col "H" esc "s")))))

