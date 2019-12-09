(ns liq2.extras.cool-stuff
  (:require [clojure.string :as str]
            [liq2.editor :as editor]
            [liq2.buffer :as buffer]))

(defn output-snapshot
  [] 
  (let [id (editor/get-buffer-id-by-name "output-snapshot")]
    (if id
      (editor/switch-to-buffer id)
      (editor/new-buffer "" {:major-mode :clojure-mode :name "output-snapshot"}))
    (editor/apply-to-buffer
      #(-> %
           buffer/clear
           (buffer/insert-string
             (buffer/get-text (editor/get-buffer "output")))))))
 

(defn load-cool-stuff
  []
  (editor/add-key-bindings :clojure-mode :normal {"C-o" output-snapshot}))  