(ns liq2.extras.markdownfolds
  (:require [clojure.string :as str]
            [liq2.editor :as editor]
            [liq2.buffer :as buffer]
            [liq2.util :as util]))

(defn get-headline-level
  ([buf row]
   (let [line (buffer/get-line buf row)]
     (cond (re-find #"^ [✔☐➜✘] " line) 10  
           (re-find #"^.def?n" line) 10  
           true (count (re-find #"^[#]+(?= )" line)))))
  ([buf]
   (get-headline-level buf (-> buf ::buffer/cursor ::buffer/row))))

(defn get-level-end
  ([buf row]
   (let [l (get-headline-level buf row)
         lc (buffer/line-count buf)]
     (if (= l 0)
       row
       (loop [r (inc row)]
         (cond (= r lc) r
               (<= 1 (get-headline-level buf r) l) (dec r)
               true (recur (inc r)))))))
  ([buf]
   (get-level-end buf (-> buf ::buffer/cursor ::buffer/row))))

(defn hide-lines-below
  ([buf p n]
   (update buf ::buffer/hidden-lines assoc (inc (p ::buffer/row)) (+ (p ::buffer/row) n)))
  ([buf n]
   (hide-lines-below buf (-> buf ::buffer/cursor) n)))

(defn show-lines-below
 ([buf p]
  (update buf ::buffer/hidden-lines dissoc (inc (p ::buffer/row))))
 ([buf]
  (show-lines-below buf (buf ::buffer/cursor))))

(defn hide-level
  ([buf p]
    (let [row (p ::buffer/row)]
      (update buf ::buffer/hidden-lines assoc (inc row) (get-level-end buf row))))
  ([buf]
    (hide-level buf (buf ::buffer/cursor))))

(defn hide-all-levels
  [buf]
  (loop [b buf row 1]
    (if (> row (buffer/line-count b))
      b
      (recur (hide-level b {::buffer/row row ::buffer/col 1}) (inc row)))))

(defn toggle-show-lines-below
  [buf]
  (if (not= (buffer/next-visible-row buf) (inc (-> buf ::buffer/cursor ::buffer/row)))
    (show-lines-below buf)
    (hide-lines-below buf (- (get-level-end buf) (-> buf ::buffer/cursor ::buffer/row)))))

(defn load-markdownfolds
  []
  (editor/add-key-bindings
    :fundamental-mode
    :normal {"+" {"+" (fn [] (editor/apply-to-buffer #(toggle-show-lines-below %)))
                  "a" (fn [] (editor/apply-to-buffer hide-all-levels))
                  "t" (fn [] (editor/apply-to-buffer hide-level))
                  "-" (fn [] (editor/apply-to-buffer #(show-lines-below %)))}}
  ))
