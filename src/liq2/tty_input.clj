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

(defn- tty-println
  [& args]
  (.println (System/out) (str/join "" args)))

(defn set-raw-mode
  []
  (cmd "/bin/sh" "-c" "stty -echo raw </dev/tty")
  (tty-print esc "0;37m" esc "2J")
  (tty-print esc "?7l"))  ; disable line wrap


(defn set-line-mode
  []
  ;(tty-print esc "0;37m" esc "2J")
  (cmd "/bin/sh" "-c" "stty -echo cooked </dev/tty")
  (cmd "/bin/sh" "-c" "stty -echo sane </dev/tty")
  (tty-print esc "0;0H" esc "s"))

(defn rows
  []
  (loop [shellinfo (with-out-str (cmd "/bin/sh" "-c" "stty size </dev/tty")) n 0]
    (if (or (re-find #"^\d+" shellinfo) (> n 10)) 
      (Integer/parseInt (re-find #"^\d+" shellinfo))
      (do
        (tty-println n)
        (Thread/sleep 100)
        (recur (with-out-str (cmd "/bin/sh" "-c" "stty size </dev/tty")) (inc n))))))

(defn cols
  []
  (loop [shellinfo (with-out-str (cmd "/bin/sh" "-c" "stty size </dev/tty")) n 0]
    (if (or (re-find #"\d+$" shellinfo) (> n 10)) 
      (Integer/parseInt (re-find #"\d+$" shellinfo))
      (do
        (tty-println n)
        (Thread/sleep 100)
        (recur (with-out-str (cmd "/bin/sh" "-c" "stty size </dev/tty")) (inc n))))))

(defn exit-handler
  []
  (tty-print "\033[0;37m\033[2J")
  (tty-print "\033[?25h")
  (cmd "/bin/sh" "-c" "stty -echo cooked </dev/tty")
  (cmd "/bin/sh" "-c" "stty -echo sane </dev/tty")
  (tty-print "\n")
  (System/exit 0))

(defn init
  []
  (tty-print esc "0;0H" esc "s")
  (set-raw-mode))

(defn input-handler
  [fun]
  ;(tty-print esc "0;37m" esc "2J")
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

