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


(defn get-folder
  [filepath]
  (str (.getParent (io/file filepath))))

(defn resolve-path
  [part alternative-parent]
  (cond (.isAbsolute (io/file part)) (.getCanonicalPath (io/file part))
        (re-find #"^~" part) (str (.getCanonicalPath (io/file (str/replace part #"^~" (System/getProperty "user.home")))))
        true (str (.getCanonicalPath (io/file alternative-parent part)))))

(defn file
  ([folder filename]
    (str (io/file folder filename)))
  ([filepath]
    (str (io/file filepath))))

(defn filename
  [filepath]
  (str (.getName (io/file filepath))))

(defn parent
  [filepath]
  ;; if root return nil
  )

(defn absolute
  [filepath]
  (.getAbsolutePath (io/file filepath)))

(defn canonical
  [filepath]
  (.getCanonicalPath (io/file filepath)))

(defn folder?
  [filepath]
  (.isDirectory (io/file filepath)))

(defn file?
  [filepath]
  (.isFile (io/file filepath)))

(defn exists?
  [filepath]
  (.exists (io/file filepath)))

(defn tmp-file
  [filename]
  (str (io/file (System/getProperty "java.io.tmpdir") filename)))

(defn get-roots
  []
  (map str (java.io.File/listRoots)))

(defn get-children
  [filepath]
  (map str (.listFiles (io/file filepath))))

(defn get-folders
  [filepath]
  (map str (filter #(.isDirectory %) (.listFiles (io/file filepath)))))

(defn get-files
  [filepath]
  (filter file? (map str (.listFiles (io/file filepath)))))

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

(comment
  (get-folder "/tmp/tmp.clj")
  (ls-files "/tmp")
  (ls-files "~")
  (ls-folders "/tmp")
  (ls-folders "~")
  (get-parent-folder "~")
  )

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
