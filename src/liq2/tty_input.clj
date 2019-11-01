(ns liq2.tty-input
  (:require [clojure.string :as str]
            [liq2.tty-shared :as shared]))

(def esc "\033[")

; https://github.com/mogenslund/liquidjs/blob/master/src/dk/salza/liq/adapters/tty.cljs
(defn cmd
  [& args]
  (.waitFor (.exec (Runtime/getRuntime) (into-array args))))

(defn- tty-print
  [& args]
  (.print (System/out) (str/join "" args)))

(defn set-raw-mode
  []
  (cmd "/bin/sh" "-c" "stty -echo raw </dev/tty")
  (tty-print esc "0;37m" esc "2J")
  (tty-print esc "?7l"))  ; disable line wrap


(defn set-line-mode
  []
  (tty-print esc "0;37m" esc "2J")
  (cmd "/bin/sh" "-c" "stty -echo cooked </dev/tty")
  (cmd "/bin/sh" "-c" "stty -echo sane </dev/tty")
  (tty-print esc "0;0H" esc "s"))

(defn input-handler
  [fun]
  (set-raw-mode)
  (tty-print esc "0;37m" esc "2J")
  (tty-print esc "0;0H" esc "s")
  (future
    (let [r (java.io.BufferedReader. *in*)
          read-input (fn [] (shared/raw2keyword
                              (let [input0 (.read r)]
                                (if (= input0 27)
                                  (loop [res (list input0)]
                                    (Thread/sleep 1)
                                    (if (not (.ready r))
                                      (reverse res)
                                      (recur (conj res (.read r)))))
                                 input0))))]
      (loop [input (read-input)]
        (when (not= input "C-q")
          (fun input)
          (recur (read-input)))))
    (shutdown-agents)
    (set-line-mode)))

