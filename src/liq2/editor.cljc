(ns liq2.editor
  (:require [clojure.string :as str]
            #?(:cljs [lumo.io :as io :refer [slurp spit]])
            [liq2.datastructures.sub-editor :as se]))

(def state (atom {::tmp (se/sub-editor "abc\ndef")
                  ::output-handler nil}))

(defn set-output-handler
  [output-handler]
  (swap! state assoc ::output-handler output-handler))

(defn get-current-sub-editor
  []
  (@state ::tmp))

(defn push-output
  []
  ((@state ::output-handler) (get-current-sub-editor) "...")) 

(defn handle-input
  [c]
  ;(spit "/tmp/liq2.log" (str "INPUT: " c "\n") :append true)
  (swap! state update ::tmp #(se/insert-char % (first c)))
  (push-output))

