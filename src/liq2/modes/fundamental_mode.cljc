(ns liq2.modes.fundamental-mode
  (:require [clojure.string :as str]
            [liq2.datastructures.sub-editor :as se]))

(def mode
  {:insert {"esc" {:function #(se/set-mode % :normal) :type :sub-editor}}
   :normal {"i" {:function #(se/set-mode % :insert) :type :sub-editor}
            "h" {:function se/backward-char :type :sub-editor}
            "j" {:function se/next-line :type :sub-editor}
            "k" {:function se/previous-line :type :sub-editor}
            "l" {:function se/forward-char :type :sub-editor}
            "0" {:function se/beginning-of-line :type :sub-editor}
            "$" {:function se/end-of-line :type :sub-editor}}})