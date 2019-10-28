(ns liq2.experiments.core
  (:require [clojure.string :as str]
            [liq2.experiments.editor :as editor]
            [liq2.experiments.tty-input :as input]
            [liq2.experiments.tty-output :as output]))

;; clj -m liq2.experiments.core
(defn -main
  []
  (editor/set-output-handler output/output-handler)
  (input/input-handler editor/handle-input))
