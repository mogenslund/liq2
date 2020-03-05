(ns liq2.modes.spacemacs-mode
  (:require [clojure.string :as str]
            [liq2.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq2.buffer :as buffer]
            [liq2.util :as util]))

;  (editor/set-spacekey ["m"] "Clojure commands" nil)
;  (editor/set-spacekey ["m" "e"] "Evaluation" nil)
;  (editor/set-spacekey ["m" "e" "e"] "eval-last-sexp" editor/eval-last-sexp)
;  (editor/set-spacekey ["m" "e" "b"] "eval-buffer" editor/evaluate-file)
;  (editor/set-spacekey ["m" "g"] "Goto" nil)
;  (editor/set-spacekey ["m" "g" "g"] "Goto definition" editor/context-action)
;  (editor/set-spacekey ["m" ","] "Highligt sexp" editor/highlight-sexp-at-point)
;  (editor/set-spacekey ["f"] "Files" nil)
;  (editor/set-spacekey ["f" "f"] "find-file" #(findfileapp/run textapp/run))
;  (editor/set-spacekey ["f" "s"] "save-file" editor/save-file)
;  (editor/set-spacekey ["f" "r"] "Reload file" editor/force-reopen-file)
;  (editor/set-spacekey ["w"] "Window" nil)
;  (editor/set-spacekey ["w" "/"] "Split window vertically" editor/split-window-right)
;  (editor/set-spacekey ["w" "-"] "Split window horizontally" editor/split-window-below)
;  (editor/set-spacekey ["w" "d"] "Delete window" editor/delete-window)
;  (editor/set-spacekey ["w" "w"] "Other window" editor/other-window)
;  (editor/set-spacekey ["q"] "Quit" nil)
;  (editor/set-spacekey ["q" "q"] "Quit" editor/quit)
;  (editor/set-spacekey ["\t"] "Last buffer" editor/previous-real-buffer)
;  (editor/set-spacekey [" "] "Command typeahead" #(do (editor/request-fullupdate) (commandapp/run)))

(def mode
  {:normal {" " {:description "m Clojure commands\nf Files\nw Window\nq Quit"
                 "m" {:description "e Evaluation\ng Goto"
                      "e" {:description "e eval-last-sexp"
                           "e" :eval-sexp-at-point}}
                 "f" {:description "f find-file"
                      "f" :Ex}}}})
 
