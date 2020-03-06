(ns liq2.modes.spacemacs-mode
  (:require [clojure.string :as str]
            [liq2.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq2.buffer :as buffer]
            [liq2.util :as util]))

;; https://gist.github.com/rnwolf/e09ae9ad6d3ac759767d129d52cab1f1

(defn add-description
   [keys description]
   (swap! editor/state
          assoc-in
          (concat [::editor/modes :spacemacs-mode :normal] keys [:description])
          description))

(defn add-mapping
   [keys fun]
   (swap! editor/state
          assoc-in
          (concat [::editor/modes :spacemacs-mode :normal] keys)
          fun))


(defn load-spacemacs-mode
  []
  (editor/add-mode :spacemacs-mode {:normal {}})
  (add-description [" "] "m Clojure commands    f Files   b Buffers   q Quit")
  (add-description [" " "m"] "e Evaluation      g Goto")
  (add-description [" " "m" "e"] "e eval-last-sexp")
  (add-mapping [" " "m" "e" "e"] :eval-sexp-at-point)
  (add-description [" " "f"] "f Find file     s Save file")
  (add-mapping [" " "f" "f"] :Ex)
  (add-mapping [" " "f" "s"] :w)
  (add-description [" " "q"] "q Quit")
  (add-mapping [" " "q" "q"] :q)
  (add-description [" " "b"] "TAB Alternate buffer      b Bufferchooser    k Kill buffer ")
  (add-mapping [" " "b" "\t"] :previous-regular-buffer)
  (add-mapping [" " "b" "b"] #(((editor/get-mode :buffer-chooser-mode) :init)))
  (add-mapping [" " "b" "k"] :bd))
 
 
