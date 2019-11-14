(ns liq2.util
  (:require [clojure.string :as str]
            #?(:clj [clojure.java.io :as io]
               :cljs [lumo.io :as io :refer [slurp spit]])
            #?(:cljs [cljs.js :refer [eval eval-str empty-state]])))



(def counter (atom 0))
(defn counter-next
  []
  (swap! counter inc))

(defn now
  "Current time in milliseconds"
  []
  #?(:clj (System/currentTimeMillis)
     :cljs (.getTime (js/Date.))))

(defn sleep
  ""
  [ms]
  #?(:clj (Thread/sleep ms)
     :cljs (do)))

(defn read-file
  [path]
  #?(:clj (when (.exists (io/file path))
            (slurp path))
     :cljs (slurp path)))

(defn write-file
  [path content]
  (spit path content))

(defn eval-safe
  [text]
  #?(:clj (try
            (load-string text)
            (catch Exception e (str e)))
     :cljs (do (set! cljs.js/*eval-fn* cljs.js/js-eval) (eval-str (empty-state) text str))))

;(defn clipboard-content
;  []
;  (if (java.awt.GraphicsEnvironment/isHeadless)
;    @localclipboard
;    (let [clipboard (.getSystemClipboard (java.awt.Toolkit/getDefaultToolkit))]
;      (try
;        (.getTransferData (.getContents clipboard nil) (java.awt.datatransfer.DataFlavor/stringFlavor))
;        (catch Exception e "")))))

;(defn set-clipboard-content
;  [text]
;  (reset! localclipboard text)
;  (when (not (java.awt.GraphicsEnvironment/isHeadless))
;    (let [clipboard (.getSystemClipboard (java.awt.Toolkit/getDefaultToolkit))]
;      (.setContents clipboard (java.awt.datatransfer.StringSelection. text) nil))))
