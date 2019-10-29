(ns liq2.core
  (:require [clojure.string :as str]
            [liq2.modes.fundamental-mode :as fundamental-mode]
            [liq2.editor :as editor]
            [liq2.tty-input :as input]
            [liq2.tty-output :as output]))

;; clj -m liq2.experiments.core
(defn -main
  []
  (editor/add-mode :fundamental-mode fundamental-mode/mode)
  (editor/set-output-handler output/output-handler)
  (input/input-handler editor/handle-input)
  (editor/push-output))