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
  (input/init)
  (let [rows 16 ; (input/rows)
        cols 80]  ;(input/cols)
    (editor/add-mode :fundamental-mode fundamental-mode/mode)
    (editor/add-mode :minibuffer-mode minibuffer-mode/mode)
    (editor/new-frame 1 1 (dec rows) cols)
    (editor/new-buffer "" {})
    (editor/set-output-handler output/output-handler)
    (editor/set-exit-handler input/exit-handler)
    (input/input-handler editor/handle-input)
    (editor/push-output)
    (editor/new-frame rows 1 1 cols)
    (editor/apply-to-buffer (editor/new-buffer "" {:name "-minibuffer-"}) #(-> % (buffer/set-major-mode :minibuffer-mode) (buffer/set-mode :insert)))
    (editor/previous-buffer)
    (editor/push-output)))


