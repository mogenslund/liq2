(ns liq2.experiments.tty-output
  (:require [clojure.string :as str]))

(def esc "\033[")

(defn output-sub-editor
  [se frame]
  (println se frame))
