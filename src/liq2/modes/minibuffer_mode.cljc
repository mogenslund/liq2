(ns liq2.modes.minibuffer-mode
  (:require [clojure.string :as str]
            [liq2.editor :as editor]
            [liq2.buffer :as buffer]))

(defn execute
  []
  (let [content (buffer/get-text (editor/get-current-buffer))]
    (editor/apply-to-buffer buffer/clear)
    (editor/push-output)
    (editor/previous-buffer)
    (when (= content ":q")
      (editor/exit-program))))

(def mode
  {:insert {"esc" editor/previous-buffer
            "\n" execute}
   :normal {"esc" {:function #(buffer/set-mode % :insert) :type :buffer}
            "l" {:function buffer/forward-char :type :buffer}}})