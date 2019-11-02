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

;(def tow (atom {:row 1 :col 1})) ; TMP: Until frame is implemented

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
        tow (recalculate-tow buf rows cols (or (@tow-cache (frame/get-id fr)) {:row 1 :col 1}))
        crow (buffer/get-row buf)
        ccol (buffer/get-col buf)]
  (swap! tow-cache assoc (frame/get-id fr) tow)
  ;(tty-print esc "?25l") ; Hide cursor
  (loop [trow top tcol left row (tow :row) col (tow :col) cursor-row nil cursor-col nil]
    (if (< trow (+ rows top))
      (do
      ;; Check if row has changed...
        (let [c (or (buffer/get-char buf row col)
                    (if (and (= col 1) (> row (buffer/line-count buf))) (str esc "36m~" esc "0m") \space))
              cursor-match (or (and (= row crow) (= col ccol))
                               (and (not cursor-row) (> row crow)))
              new-cursor-row (if cursor-match trow cursor-row)
              new-cursor-col (if cursor-match tcol cursor-col)]
          (when (not= c (@cache [trow tcol]))
            (tty-print esc trow ";" tcol "H" esc "s" c)
            (swap! cache assoc [trow tcol] c))
          (if (> tcol cols)
            (if (> (buffer/col-count buf row) col) 
              (recur (inc trow) left row (inc col) new-cursor-row new-cursor-col) 
              (recur (inc trow) left (inc row) 1 new-cursor-row new-cursor-col))
            (recur trow (inc tcol) row (inc col) new-cursor-row new-cursor-col))))
      (tty-print esc "?25h" esc cursor-row ";" cursor-col "H" esc "s")))))

