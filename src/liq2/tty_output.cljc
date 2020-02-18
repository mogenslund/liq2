(ns liq2.tty-output
  (:require [liq2.buffer :as buffer]
            #?(:clj [clojure.java.io :as io]
              ; :cljs [lumo.io :as io]
              )
            [clojure.string :as str]))

(def ^:private last-buffer (atom nil))
(def esc "\033[")

(defn cmd
  "Execute a native command.
  Adding :timeout 60 or similar as last command will
  add a timeout to the process."
  [& args]
  (let [builder (doto (ProcessBuilder. args)
                  (.redirectErrorStream true))
        process (.start builder)
        lineprocessor (future (doseq [line (line-seq (io/reader (.getInputStream process)))]
                                (println line)))
        monitor (future (.waitFor process))
        starttime (quot (System/currentTimeMillis) 1000)]
    (try
      (while (and (not (future-done? monitor))
                  (< (- (quot (System/currentTimeMillis) 1000) starttime)))
        (Thread/sleep 1000))
      (catch Exception e
        (do (.destroy process)
            (println "Exception" (.getMessage e))
            (future-cancel monitor))))
    (when (not (future-done? monitor))
      (println "TimeoutException or Interrupted")
      (.destroy process))))

(defn- tty-print
  [& args]
  #?(:clj (.print (System/out) (str/join "" args))
     :cljs (js/process.stdout.write (str/join "" args))))

(defn- tty-println
  [& args]
  #?(:clj (.println (System/out) (str/join "" args))
     :cljs (js/process.stdout.write (str (str/join "" args) "\n"))))

(defn rows
  []
  #?(:clj (loop [shellinfo (with-out-str (cmd "/bin/sh" "-c" "stty size </dev/tty")) n 0]
            (if (or (re-find #"^\d+" shellinfo) (> n 10)) 
              (Integer/parseInt (re-find #"^\d+" shellinfo))
              (do
                (tty-println n)
                (Thread/sleep 100)
                (recur (with-out-str (cmd "/bin/sh" "-c" "stty size </dev/tty")) (inc n)))))
     :cljs (aget (js/process.stdout.getWindowSize) 1))) 

(defn cols
  []
  #?(:clj (loop [shellinfo (with-out-str (cmd "/bin/sh" "-c" "stty size </dev/tty")) n 0]
            (if (or (re-find #"\d+$" shellinfo) (> n 10)) 
             (dec (Integer/parseInt (re-find #"\d+$" shellinfo)))
             (do
               (tty-println n)
               (Thread/sleep 100)
               (recur (with-out-str (cmd "/bin/sh" "-c" "stty size </dev/tty")) (inc n)))))
     :cljs (aget (js/process.stdout.getWindowSize) 0))) 

(defn get-dimensions
  []
  {:rows (rows) :cols (cols)})

(defn buffer-footprint
  [buf]
  [(buf ::buffer/window) (buf ::buffer/name) (buf ::buffer/file-name)])

;(defn calculate-wrapped-row-dist
;  [buf cols row1 row2]
;  (reduce #(+ 1 %1 (quot (dec (buffer/col-count buf %2)) cols)) 0 (range row1 row2))) 

;(defn recalculate-tow
;  "This is a first draft, which does not handle edge
;  cases with very long lines and positioning logic."
;  [buf rows cols tow1]
;  (cond (< (-> buf ::buffer/cursor ::buffer/row) (tow1 :row)) (assoc tow1 :row (-> buf ::buffer/cursor ::buffer/row))
;        (> (- (-> buf ::buffer/cursor ::buffer/row) (tow1 :row)) rows)
;          (recalculate-tow buf rows cols (assoc tow1 :row (- (-> buf ::buffer/cursor ::buffer/row) rows)))
;        (> (calculate-wrapped-row-dist buf cols (tow1 :row) (+ (-> buf ::buffer/cursor ::buffer/row) 1)) rows)
;          (recalculate-tow buf rows cols (update tow1 :row inc))
;        true tow1))

;(defn recalculate-tow
;  "Basic version, just to check possible speed"
;  [buf rows cols tow1]
;  (assoc tow1 :row (-> buf ::buffer/cursor ::buffer/row)))


;(comment
;  (calculate-wrapped-row-dist (buffer/buffer "aaaaaaaaaaaaaaaaaaaaaaaaaa\nbbbbbbbbbbbbbb") 10 2 3)
;  )


; Two pointers trow tcol in terminal row col in buffer

(def theme
  {:string "38;5;131"
   :keyword "38;5;117"
   :comment "38;5;105"
   :special "38;5;11"
   :green   "38;5;40"
   :yellow "38;5;11"
   :red "38;5;196"
   :definition "38;5;40"
   nil "0"})

(def char-cache (atom {}))
(defn- draw-char
  [ch row col color bgcolor]
  (let [k (str row "-" col)
        footprint (str ch row col color bgcolor)]
    (when (not= (@char-cache k) footprint)
      (tty-print esc color "m")
      (tty-print esc bgcolor "m")
      (tty-print esc row ";" col "H" esc "s" ch)
      (swap! char-cache assoc k footprint))))


(defn print-buffer
  [buf]
  (let [cache-id (buffer-footprint buf)
        w (buf ::buffer/window)
        top (w ::buffer/top)   ; Window top margin
        left (w ::buffer/left) ; Window left margin
        rows (w ::buffer/rows) ; Window rows
        cols (w ::buffer/cols) ; Window cols
        tow (buf ::buffer/tow) ; Top of window
        crow (-> buf ::buffer/cursor ::buffer/row)  ; Cursor row
        ccol (-> buf ::buffer/cursor ::buffer/col)] ; Cursor col
  (when (= cache-id @last-buffer)
    (tty-print "█")) ; To make it look like the cursor is still there while drawing.
  (tty-print esc "?25l") ; Hide cursor
  (when-let [statusline (buf :status-line)]
    (print-buffer statusline))
  ;; Looping over the rows and cols in buffer window in the terminal
  (loop [trow top  ; Terminal row
         tcol left ; Terminal col
         row (tow ::buffer/row)
         col (tow ::buffer/col)
         cursor-row nil
         cursor-col nil
         color nil
         bgcolor nil]
    (if (< trow (+ rows top))
      (do
      ;; Check if row has changed...
        (let [cursor-match (or (and (= row crow) (= col ccol))
                               (and (= row crow) (not cursor-col) (> col ccol))
                               (and (not cursor-row) (> row crow)))
              cm (or (-> buf ::buffer/lines (get (dec row)) (get (dec col))) {}) ; Char map like {::buffer/char \x ::buffer/style :string} 
              c (or (when (and cursor-match (buf :status-line)) "█") 
                    (cm ::buffer/char)
                    (if (and (= col 1) (> row (buffer/line-count buf))) (str esc "36m~" esc "0m") \space))
              new-cursor-row (if cursor-match trow cursor-row)
              new-cursor-col (if cursor-match tcol cursor-col)
              new-color (theme (cm ::buffer/style))
              new-bgcolor (if (buffer/selected? buf row col) "48;5;17" "49")
              n-trow (if (< cols tcol) (inc trow) trow)
              n-tcol (if (< cols tcol) left (inc tcol))
              n-row (cond (and (< cols tcol) (> col (buffer/col-count buf row))) (buffer/next-visible-row buf row)
                          true row)
              n-col (cond (and (< cols tcol) (> col (buffer/col-count buf row))) 1
                          true (inc col))]
             (draw-char c trow tcol new-color new-bgcolor)
;            (when (or (not= color new-color) (not= bgcolor new-bgcolor))
;              (tty-print esc new-color "m")
;              (tty-print esc new-bgcolor "m"))
;            (if (= tcol left)
;              (tty-print esc trow ";" tcol "H" esc "s" c)
;              (tty-print c))
            (when (and (= col (buffer/col-count buf row)) (> (buffer/next-visible-row buf row) (+ row 1))) (tty-print "…"))
            (recur n-trow n-tcol n-row n-col new-cursor-row new-cursor-col new-color new-bgcolor)))
      (when (buf :status-line)
        (tty-print esc cursor-row ";" cursor-col "H" esc "s" (or (buffer/get-char buf) \space))
        (tty-print esc "?25h" esc cursor-row ";" cursor-col "H" esc "s")
        (reset! last-buffer cache-id))))))

(def ^:private updater (atom nil))
(def ^:private queue (atom []))

(def ^:private next-buffer (atom nil))
#?(:cljs (js/setInterval
           #(when-let [buf @next-buffer]
              (reset! next-buffer nil)
              (print-buffer buf))
           20))

(defn printer
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
     :cljs (reset! next-buffer buf)))

(def output-handler
  {:printer printer
   :dimensions get-dimensions})