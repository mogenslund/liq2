(ns liq2.modes.minibuffer-mode
  (:require [clojure.string :as str]
            [liq2.editor :as editor]
            [liq2.buffer :as buffer]))

(defn execute
  [content]
  )

(def mode
  {:insert {"esc" editor/previous-buffer
            "enter" #(execute %)}
   :normal {"esc" {:function #(buffer/set-mode % :insert) :type :buffer}
            "l" {:function buffer/forward-char :type :buffer}}})