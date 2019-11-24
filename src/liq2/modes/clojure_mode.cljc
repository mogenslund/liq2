(ns liq2.modes.clojure-mode
  (:require [clojure.string :as str]
            [liq2.modes.fundamental-mode :as fundamental-mode]
            [liq2.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq2.buffer :as buffer]
            [liq2.util :as util]))

(def match
  {:keyword-begin #"(?<=(\s|\(|\[|\{)|^):[\w\#\.\-\_\:\+\=\>\<\/\!\?\*]+(?=(\s|\)|\]|\}|\,|$))"
   :keyword-end #".|$"
   :string-begin #"(?<!\\\\)(\")"
   :string-escape #"(\\\")"
   :string-end "\""
   :comment-begin #"(?<!\\\\);.*$|^#.*$"
   :comment-end #"$"
   :special-begin #"(?<=\()(ns |def(n|n-|test|record|protocol|macro)? )"
   :green-begin #"✔"
   :definition-begin #"[\w\#\.\-\_\:\+\=\>\<\/\!\?\*]+"
   :definition-end #"."})

(def mode
  (assoc fundamental-mode/mode
    :syntax
     {:plain ; Context
       {:style :plain1 ; style
        :matchers {(match :string-begin) :string
                   (match :keyword-begin) :keyword
                   (match :comment-begin) :comment
                   (match :special-begin) :special}}
      :string
       {:style :string
        :matchers {(match :string-escape) :string
                   (match :string-end) :string-end}}
      :string-end
       {:style :string
        :matchers {#".|$|^" :plain}}

      :comment
       {:style :comment
        :matchers {(match :comment-end) :plain}}

      :keyword
       {:style :keyword
        :matchers {(match :keyword-end) :plain}}
      
      :special
       {:style :special
        :matchers {(match :definition-begin) :definition}}

      :green
       {:style :green
        :matchers {(match :green-begin) :plain}}

      :definition
       {:style :definition
        :matchers {(match :definition-end) :plain}}}))