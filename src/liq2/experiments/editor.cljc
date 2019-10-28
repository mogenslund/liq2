(ns liq2.experiments.editor
  (:require [clojure.string :as str]
            [liq2.datastructures.sub-editor :as se]))

(def state (atom {::tmp (se/sub-editor "abc\ndef")
                  ::output-handler}))

(defn set-output-handler
  [output-handler]
  (swap! state assoc ::output-handler output-handler))

(defn get-current-sub-editor
  []
  (@state ::tmp))

(defn push-output
  []
  ((@state ::output-handler) get-current-sub-editor "...")) 

(defn handle-input
  [c]
  (swap! state update ::tmp #(se/insert-char % (first c)))
  (push-output))

