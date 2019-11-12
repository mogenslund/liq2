(ns liq2.util
  (:require [clojure.string :as str]
            #?(:clj [clojure.java.io :as io]
               :cljs [lumo.io :as io :refer [slurp spit]])))



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
  (when (.exists (io/file path))
    (slurp path)))

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
