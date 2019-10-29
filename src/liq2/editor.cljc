(ns liq2.editor
  (:require [clojure.string :as str]
            #?(:cljs [lumo.io :as io :refer [slurp spit]])
            [liq2.buffer :as buffer]))

(def state (atom {::buffers [(buffer/buffer "abc\ndef") (buffer/buffer "aaa\nbbb")]
                  ::output-handler nil
                  ::modes {}}))

(defn set-output-handler
  [output-handler]
  (swap! state assoc ::output-handler output-handler))

(defn add-mode
  [keyw mode]
  (swap! state update ::modes assoc keyw mode))

(defn get-mode
  [keyw]
  (or ((@state ::modes) keyw) {}))

(defn get-current-buffer
  []
  (-> @state ::buffers (get 0)))

(defn switch-to-buffer
  [n]
  (swap! state assoc ::buffers
         (into [] (concat [((@state ::buffers) (dec n))]
                          (subvec (@state ::buffers) 0 (dec n))
                          (subvec (@state ::buffers) n)))))

(defn push-output
  []
  ((@state ::output-handler) (get-current-buffer) "...")) 

(defn handle-input
  [c]
  (spit "/tmp/liq2.log" (str "INPUT: " c "\n"))
  (let [mode (buffer/get-mode (get-current-buffer))
        action (((get-mode :fundamental-mode) mode) c)]
    (cond (fn? action) (action)
          action (swap! state update-in [::buffers 0] (action :function))
          (= mode :insert) (swap! state update-in [::buffers 0] #(buffer/insert-char % (first c)))))
  (push-output))


