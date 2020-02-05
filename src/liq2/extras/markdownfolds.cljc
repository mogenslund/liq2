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

(defn hide-lines-below-old
  ([buf p n]
   (let [p1 (buffer/eol-point buf p)
         p2 (buffer/eol-point buf (update p ::buffer/row #(+ % n)))] 
     (buffer/hide-region buf [p1 p2])))
  ([buf n]
   (hide-lines-below buf (-> buf ::buffer/cursor) n)))


(defn show-lines-below
 ([buf p]
  (update buf ::buffer/hidden-lines dissoc (inc (p ::buffer/row))))
 ([buf]
  (show-lines-below buf (buf ::buffer/cursor))))

(defn show-lines-below1
 ([buf p]
  (buffer/unhide-region buf (buffer/eol-point buf p)))
 ([buf]
  (show-lines-below buf (-> buf ::buffer/cursor))))


(defn toggle-show-lines-below
  [buf]
  (if (not= (buffer/next-non-hidden-row buf) (inc (-> buf ::buffer/cursor ::buffer/row)))
    (show-lines-below buf)
    (hide-lines-below buf (- (get-level-end buf) (-> buf ::buffer/cursor ::buffer/row)))))

(defn toggle-show-lines-below1
  [buf]
  (if (buffer/hidden-region? buf (buffer/eol-point buf)) 
    (show-lines-below buf)
    (hide-lines-below buf (- (get-level-end buf) (-> buf ::buffer/cursor ::buffer/row)))))


(defn load-markdownfolds
  []
  (editor/add-key-bindings
    :fundamental-mode
    :normal {"+" {"+" (fn [] (editor/apply-to-buffer #(toggle-show-lines-below %)))
                  "-" (fn [] (editor/apply-to-buffer #(show-lines-below %)))}}
  ))
