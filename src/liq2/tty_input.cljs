(ns liq2.tty-input
  (:require [clojure.string :as str]
            [liq2.tty-shared :as shared]))

(def esc "\033[")

(defn- tty-print
  [& args]
  (js/process.stdout.write (str/join "" args)))

(defn set-raw-mode
  []
  (let [readline (js/require "readline")]
    (.emitKeypressEvents readline process.stdin
      (js/process.stdin.setRawMode true))))

(defn input-handler
  [fun]
  (set-raw-mode)
  (tty-print esc "0;37m" esc "2J")
  (tty-print esc "0;0H" esc "s")
  (js/process.stdin.on "keypress"
    (fn [chunk key]
      (if (not= js/key.name "q")
        (fun js/key.name)
        (js/process.exit)))))
