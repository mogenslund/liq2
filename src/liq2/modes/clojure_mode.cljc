(ns liq2.modes.clojure-mode
  (:require [clojure.string :as str]
            [liq2.modes.fundamental-mode :as fundamental-mode]
            [liq2.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq2.buffer :as buffer]
            [liq2.util :as util]))

(def match-keys
  {:match-keyword-begin #"(?<=(\s|\(|\[|\{)|^):[\w\#\.\-\_\:\+\=\>\<\/\!\?\*]+(?=(\s|\)|\]|\}|\,|$))"
   :match-keyword-end #".|$"
   :match-string-begin #"(?<!\\\\)(\")"
   :match-string-escape #"(\\\")"
   :match-string-end "\""
   :match-comment-begin #"(?<!\\\\);"
   :match-comment-end #"$"})

(def mode
  (assoc fundamental-mode/mode
    :syntax
     {:plain ; Context
       {:style :plain1 ; style
        :matchers {(match-keys :match-string-begin) :string
                   (match-keys :match-keyword-begin) :keyword
                   (match-keys :match-comment-begin) :comment}}
      :string
       {:style :string
        :matchers {(match-keys :match-string-escape) :string
                   (match-keys :match-string-end) :string-end}}
      :string-end
       {:style :string
        :matchers {#".|$|^" :plain}}
      :comment
       {:style :comment
        :matchers {(match-keys :match-comment-end) :plain}}
      :keyword
       {:style :keyword
        :matchers {(match-keys :match-keyword-end) :plain}}}))

