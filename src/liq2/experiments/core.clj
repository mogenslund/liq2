(ns liq2.experiments.core
  (:require [clojure.string :as str]
            [liq2.experiments.tty-input :as input]
            [liq2.experiments.tty-output :as output]))

(defn handle-input
  [k]
  (output/output-sub-editor k ".."))

;; clj -m liq2.experiments.core
(defn -main
  []
  (input/input-handler handle-input))
