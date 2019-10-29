(ns liq2.modes.fundamental-mode
  (:require [clojure.string :as str]
            [liq2.buffer :as buffer]))

(def mode
  {:insert {"esc" {:function #(buffer/set-mode % :normal) :type :sub-editor}}
   :normal {"i" {:function #(buffer/set-mode % :insert) :type :sub-editor}
            "h" {:function buffer/backward-char :type :sub-editor}
            "j" {:function buffer/next-line :type :sub-editor}
            "k" {:function buffer/previous-line :type :sub-editor}
            "l" {:function buffer/forward-char :type :sub-editor}
            "0" {:function buffer/beginning-of-line :type :sub-editor}
            "$" {:function buffer/end-of-line :type :sub-editor}}})