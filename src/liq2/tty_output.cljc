(ns liq2.tty-output
  (:require [liq2.buffer :as buffer]
            [clojure.string :as str]))

(def ^:private cache (atom {}))
(def ^:private tow-cache (atom {})) ; Top of window stored here, for each buffer
(def ^:private last-buffer (atom nil))
(def esc "\033[")

(defn- tty-print
  [& args]
  #?(:clj (.print (System/out) (str/join "" args))
     :cljs (js/process.stdout.write (str/join "" args))))

(defn buffer-footprint
  [buf]
  [(buffer/get-rows buf) (buffer/get-cols buf) (buffer/get-name buf) (buffer/get-filename buf)])

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

(comment
  (calculate-wrapped-row-dist (buffer/buffer "aaaaaaaaaaaaaaaaaaaaaaaaaa\nbbbbbbbbbbbbbb") 10 2 3)
  )


; Two pointers trow tcol in terminal row col in buffer

(defn print-buffer
  [buf]
  (let [cache-id (buffer-footprint buf)
        left (buffer/get-left buf)
        top (buffer/get-top buf)
        rows (buffer/get-rows buf)
        cols (buffer/get-cols buf)
        tow (recalculate-tow buf rows cols (or (@tow-cache cache-id) {:row 1 :col 1}))
        crow (buffer/get-row buf)
        ccol (buffer/get-col buf)]
  (swap! tow-cache assoc cache-id tow)
  (tty-print esc "?25l") ; Hide cursor
  (when (= cache-id @last-buffer)
    (tty-print "█")) ; To make it look like the cursor is still there while drawing.
  (loop [trow top tcol left row (tow :row) col (tow :col) cursor-row nil cursor-col nil bgcolor nil]
    (if (< trow (+ rows top))
      (do
      ;; Check if row has changed...
        (let [cursor-match (or (and (= row crow) (= col ccol))
                               (and (= row crow) (not cursor-col) (> col ccol))
                               (and (not cursor-row) (> row crow)))
              c (or (when cursor-match "█") 
                    (buffer/get-char buf row col)
                    (if (and (= col 1) (> row (buffer/line-count buf))) (str esc "36m~" esc "0m") \space))
              new-cursor-row (if cursor-match trow cursor-row)
              new-cursor-col (if cursor-match tcol cursor-col)
              new-bgcolor (if (buffer/selected? buf row col) "48;5;17" "49")]
            (when (not= bgcolor new-bgcolor) (tty-print esc new-bgcolor "m"))
            (if (= tcol left)
              (tty-print esc trow ";" tcol "H" esc "s" c)
              (tty-print c))
          ;  (swap! cache assoc [trow tcol] c))
          (if (> tcol cols)
            (if (> (buffer/col-count buf row) col) 
              (recur (inc trow) left row (inc col) new-cursor-row new-cursor-col new-bgcolor) 
              (recur (inc trow) left (inc row) 1 new-cursor-row new-cursor-col new-bgcolor))
            (recur trow (inc tcol) row (inc col) new-cursor-row new-cursor-col new-bgcolor))))
      (do
        (tty-print esc cursor-row ";" cursor-col "H" esc "s" (or (buffer/get-char buf) \space))
        (tty-print esc "?25h" esc cursor-row ";" cursor-col "H" esc "s")
        (reset! last-buffer cache-id))))))

(def ^:private updater (atom nil))
(def ^:private queue (atom []))

(defn output-handler
  [buf]
  #?(:clj (let [fp (buffer-footprint buf)]
            ;; Replace outdated versions of buf 
            (swap! queue
              (fn [q] (conj
                        (filterv #(not= (buffer-footprint %) fp) q)
                        buf)))
            (when (not @updater) (reset! updater (future nil)))
            (when (future-done? @updater)
              (reset! updater
                (future
                  (while (not (empty? @queue))
                    (when-let [b (first @queue)]
                      (swap! queue #(subvec % 1))
                      (print-buffer b)))))))
     :cljs (print-buffer buf)))
