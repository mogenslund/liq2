(ns liq2.core
  (:require [clojure.string :as str]
            [liq2.modes.fundamental-mode :as fundamental-mode]
            [liq2.modes.minibuffer-mode :as minibuffer-mode]
            [liq2.modes.buffer-chooser-mode :as buffer-chooser-mode]
            [liq2.buffer :as buffer]
            [liq2.editor :as editor]
            [liq2.tty-input :as input]
            [liq2.util :as util]
            [liq2.tty-output :as output]))

;; clj -m liq2.experiments.core
(defn -main
  []
  (input/init)
  (let [rows (input/rows)
        cols (input/cols)]
    (editor/add-mode :fundamental-mode fundamental-mode/mode)
    (editor/add-mode :minibuffer-mode minibuffer-mode/mode)
    (editor/add-mode :buffer-chooser-mode buffer-chooser-mode/mode)
    (editor/set-output-handler output/output-handler)
    (editor/set-exit-handler input/exit-handler)
    (input/input-handler editor/handle-input)
    ;(editor/paint-buffer)
    (editor/new-buffer ""
                       {:name "*minibuffer*" :top rows :left 1 :rows 1 :cols cols
                        :major-mode :minibuffer-mode :mode :insert})
    (editor/new-buffer "Output" {:name "*output*" :top (- rows 5) :left 1 :rows 5 :cols cols :mode :insert})
    (editor/new-buffer "-----------------------------" {:name "*delimeter*" :top (- rows 6) :left 1 :rows 1 :cols cols})
    (editor/new-buffer "" {:top 1 :left 1 :rows (- rows 7) :cols cols})
    (editor/paint-buffer)))


