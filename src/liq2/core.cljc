(ns liq2.core
  (:require [clojure.string :as str]
            [liq2.modes.fundamental-mode :as fundamental-mode]
            [liq2.modes.minibuffer-mode :as minibuffer-mode]
            [liq2.buffer :as buffer]
            [liq2.editor :as editor]
            [liq2.tty-input :as input]
            [liq2.tty-output :as output]))

;; clj -m liq2.experiments.core
(defn -main
  []
  (editor/add-mode :fundamental-mode fundamental-mode/mode)
  (editor/add-mode :minibuffer-mode minibuffer-mode/mode)
  (editor/new-frame 1 1 10 80)
  (editor/new-buffer)
  (editor/set-output-handler output/output-handler)
  (input/input-handler editor/handle-input)
  (editor/push-output)
  (editor/new-frame 11 1 1 80)
  (editor/apply-to-buffer (editor/new-buffer) #(-> % (buffer/set-major-mode :minibuffer-mode) (buffer/set-mode :insert)))
  (editor/previous-buffer)
  (editor/push-output))


