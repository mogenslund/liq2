(ns liq2.modes.minibuffer-mode
  (:require [clojure.string :as str]
            [liq2.util :as util]
            [liq2.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq2.buffer :as buffer]))

(defn set-output
  [s]
  (editor/apply-to-buffer "*output*" #(-> % buffer/clear (buffer/insert-string s)))
  (editor/paint-buffer (get-buffer "*output*")))

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
    (cond (<= (count content) 1) (do)
          (= content ":q") (editor/exit-program)
          (= content ":bnext") (editor/oldest-buffer)
          (= content ":new") (editor/new-buffer "" {})
          (= content ":buffers") (((editor/get-mode :buffer-chooser-mode) :init)) 
          (= content ":w") (write-file) 
          (= content ":t") (editor/open-file "/home/sosdamgx/proj/liquid/src/dk/salza/liq/slider.clj")
          (= content ":t1") (editor/highlight-buffer)
          (= content ":t2") (set-output (buffer/get-word (editor/get-current-buffer)))
          (= content ":t3") (((editor/get-mode :typeahead-mode) :init) ["aaa" "bbb" "aabb" "ccc"] str buffer/insert-string) 
          (= content ":e .") (((editor/get-mode :dired-mode) :init))
          (re-matches #":e .*" content) (editor/open-file (subs content 3))
          (= (subs content 0 1) "/") (apply-to-buffer #(buffer/search % (subs content 1))))))

(def mode
  {:insert {"esc" editor/previous-buffer
            "backspace" (fn [] (apply-to-buffer #(if (> (buffer/get-col %) 1) (-> % buffer/left buffer/delete-char) %)))
            "\n" execute}
   :normal {"esc" (fn [] (apply-to-buffer #(buffer/set-mode % :insert)))
            "l" #(apply-to-buffer buffer/right)}})