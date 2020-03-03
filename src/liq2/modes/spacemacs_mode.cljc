(ns liq2.modes.spacemacs-mode
  (:require [clojure.string :as str]
            [liq2.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq2.buffer :as buffer]
            [liq2.util :as util]))

(def mode
  {:normal {" " {"m" {"e" {"e" :eval-sexp-at-point}}}}})
 
