(ns liq2.core
  (:require [clojure.string :as str]
            [liq2.modes.fundamental-mode :as fundamental-mode]
            [liq2.editor :as editor]
            [liq2.tty-input :as input]
            [liq2.tty-output :as output]))

;; clj -m liq2.experiments.core
(defn -main
  []
  ;(editor/new-frame 10 10 20 30)
  (editor/new-frame 1 1 5 10)
  (editor/new-buffer)
  ;(editor/new-frame 10 50 20 30)
  (editor/new-buffer)
  (editor/add-mode
    :fundamental-mode
    (assoc-in fundamental-mode/mode [:normal "m"] (fn [] (editor/previous-buffer 1))))
  (editor/set-output-handler output/output-handler)
  (input/input-handler editor/handle-input)
  (editor/push-output))

