(ns liq2.core
  (:require [clojure.string :as str]
            [liq2.modes.fundamental-mode :as fundamental-mode]
            [liq2.modes.minibuffer-mode :as minibuffer-mode]
            [liq2.modes.buffer-chooser-mode :as buffer-chooser-mode]
            [liq2.modes.dired-mode :as dired-mode]
            [liq2.modes.typeahead-mode :as typeahead-mode]
            [liq2.modes.clojure-mode :as clojure-mode]
            [liq2.modes.info-dialog-mode :as info-dialog-mode]
            #?(:clj [liq2.extras.cool-stuff :as cool-stuff])
            [liq2.extras.snake-mode :as snake-mode]
            [liq2.buffer :as buffer]
            [liq2.editor :as editor]
            [liq2.tty-input :as input]
            [liq2.util :as util]
            [liq2.commands :as commands]
            [liq2.tty-output :as output])
  #?(:clj (:gen-class)))

(defn load-dot-liq2
  []
  (try
    (let [path (util/resolve-home "~/.liq2")]
      (when (util/exists? path)
        (load-file path)))
   (catch Exception e (editor/message (str "Error loading .liq2:\n" e)))))

(defn load-extras
  []
  #?(:clj (cool-stuff/load-cool-stuff))
  (swap! editor/state assoc-in [:liq2.editor/modes :snake-mode] liq2.extras.snake-mode/mode)
  (swap! editor/state assoc-in [:liq2.editor/commands :snake] liq2.extras.snake-mode/run))

;; clj -m liq2.experiments.core
(defn -main
  []
  (input/init)
  (swap! editor/state update ::editor/commands merge commands/commands)
  (editor/add-mode :fundamental-mode fundamental-mode/mode)
  (editor/add-mode :minibuffer-mode minibuffer-mode/mode)
  (editor/add-mode :buffer-chooser-mode buffer-chooser-mode/mode)
  (editor/add-mode :typeahead-mode typeahead-mode/mode)
  (editor/add-mode :dired-mode dired-mode/mode)
  (editor/add-mode :clojure-mode clojure-mode/mode)
  (editor/add-mode :info-dialog-mode info-dialog-mode/mode)
  (editor/set-output-handler output/output-handler)
  (editor/set-exit-handler input/exit-handler)
  (input/input-handler editor/handle-input)
  (let [w (editor/get-window)
        rows (w ::buffer/rows)
        cols (w ::buffer/cols w)]
    ;(editor/paint-buffer)
    (editor/new-buffer "" {:name "*status-line*" :top rows :left 1 :rows 1 :cols cols
                        :major-mode :fundamental-mode :mode :insert})
    (editor/new-buffer ""
                       {:name "*minibuffer*" :top rows :left 1 :rows 1 :cols cols
                        :major-mode :minibuffer-mode :mode :insert})
    ;(editor/new-buffer "Output" {:name "output" :top (- rows 5) :left 1 :rows 5 :cols cols :mode :normal})
    ;(editor/new-buffer "-----------------------------" {:name "*delimeter*" :top (- rows 6) :left 1 :rows 1 :cols cols})
    ;(editor/new-buffer "" {:top 1 :left 1 :rows (- rows 7) :cols cols :major-mode :clojure-mode})
    (editor/new-buffer "" {:name "output" :top 1 :left 1 :rows (- rows 1) :cols cols :mode :normal})
    (editor/new-buffer "" {:name "scratch" :top 1 :left 1 :rows (- rows 1) :cols cols :major-mode :clojure-mode})
    (editor/paint-buffer)
    (load-extras)
    #?(:clj (load-dot-liq2))))

#?(:cljs (set! cljs.core/*main-cli-fn* -main))