(ns liq2.modes.minibuffer-mode
  (:require [clojure.string :as str]
            [liq2.util :as util]
            [liq2.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq2.buffer :as buffer]))

(defn open-file
  [path]
  (editor/new-buffer (or (util/read-file path) "") {:name path :filename path}))

(defn write-file
  []
  (let [buf (editor/get-current-buffer)]
    (when-let [f (buffer/get-filename buf)]
      (util/write-file f (buffer/get-text buf)))))

(defn execute
  []
  (let [content (buffer/get-text (editor/get-current-buffer))]
    (apply-to-buffer buffer/clear)
    (editor/paint-buffer)
    (editor/previous-buffer)
    (cond (= content ":q") (editor/exit-program)
          (= content ":bnext") (editor/oldest-buffer)
          (= content ":new") (editor/new-buffer "" {})
          (= content ":buffers") (((editor/get-mode :buffer-chooser-mode) :init)) 
          (= content ":w") (write-file) 
          (= content ":t") (open-file "/home/sosdamgx/proj/liquid/src/dk/salza/liq/slider.clj")
          (= content ":t1") (editor/highlight-buffer)
          (re-matches #":e .*" content) (open-file (subs content 3)))))

(def mode
  {:insert {"esc" editor/previous-buffer
            "backspace" (fn [] (apply-to-buffer #(if (> (buffer/get-col %) 1) (-> % buffer/backward-char buffer/delete-char) %)))
            "\n" execute}
   :normal {"esc" (fn [] (apply-to-buffer #(buffer/set-mode % :insert)))
            "l" #(apply-to-buffer buffer/forward-char)}})