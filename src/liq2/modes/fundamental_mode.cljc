(ns liq2.modes.fundamental-mode
  (:require [clojure.string :as str]
            [liq2.editor :as editor]
            [liq2.buffer :as buffer]))

(def mode
  {:insert {"esc" {:function #(-> % (buffer/set-mode :normal) buffer/backward-char) :type :buffer}}
   :normal {"i" {:function #(buffer/set-mode % :insert) :type :buffer}
            "h" {:function buffer/backward-char :type :buffer}
            "j" {:function buffer/next-line :type :buffer}
            "k" {:function buffer/previous-line :type :buffer}
            "l" {:function buffer/forward-char :type :buffer}
            "0" {:function buffer/beginning-of-line :type :buffer}
            "$" {:function buffer/end-of-line :type :buffer}
            "x" {:function buffer/delete-char :type :buffer}
            "/" editor/previous-buffer
            "o" {:function buffer/append-line :type :buffer}}})