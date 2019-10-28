(ns liq2.experiments.tty-output
  (:require [liq2.datastructures.sub-editor :as se]
            [clojure.string :as str]))

(def esc "\033[")

(defn output-handler
  [se frame]
  (println se frame))
