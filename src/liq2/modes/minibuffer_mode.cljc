(ns liq2.modes.minibuffer-mode
  (:require [clojure.string :as str]
            #?(:clj [clojure.java.io :as io]
               :cljs [lumo.io :as io :refer [slurp spit]])
            [liq2.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq2.buffer :as buffer]))

(defn load-file-content
  [buf path]
  (if (and (.exists (io/file path)) (.isFile (io/file path)))
    (-> buf
        buffer/clear
        (buffer/set-filename path)
        (buffer/insert-string (slurp path))
        buffer/beginning-of-buffer)
    buf))

(defn execute
  []
  (let [content (buffer/get-text (editor/get-current-buffer))]
    (apply-to-buffer buffer/clear)
    (editor/push-output)
    (editor/previous-buffer)
    (cond (= content ":q") (editor/exit-program)
          (= content ":bnext") (editor/oldest-buffer)
          (= content ":new") (editor/new-buffer "" {})
          (= content ":t") (apply-to-buffer #(load-file-content % "/home/sosdamgx/proj/liquid/src/dk/salza/liq/slider.clj"))
          (re-matches #":e .*" content) (apply-to-buffer #(load-file-content % (subs content 3))))))

(def mode
  {:insert {"esc" #(apply-to-buffer editor/previous-buffer)
            "backspace" (fn [] (apply-to-buffer #(if (> (buffer/get-col %) 1) (-> % buffer/backward-char buffer/delete-char) %)))
            "\n" execute}
   :normal {"esc" (fn [] (apply-to-buffer #(buffer/set-mode % :insert)))
            "l" #(apply-to-buffer buffer/forward-char)}})