(ns liq2.experiments.tty-input
  (:require [clojure.string :as str]))

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


(defn- raw2keyword
  [raw]
  (if (integer? raw)
    (cond (= raw 127) "backspace"
          (>= raw 32) (str (char raw))
          (= raw 9) "\t"
          (= raw 13) "\n"
          (<= 1 raw 26) (str "C-" (char (+ raw 96)))
          (= raw 0) "C- "
          true (str (char raw)))
    (let [raw2 (conj (take-while #(not= % 27) (rest raw)) 27)
          c0 (first raw)
          c1 (second raw)
          n (count raw2)]
      (cond (and (= n 1) (= c0 27)) "esc"
            (and (= n 2) (>= c1 32)) (str "M-" (char c1))
            (and (= n 2) (= c1 13)) "M-\n"
            (and (= n 2) (= c0 27) (<= 1 c1 26)) (str "C-M-" (char (+ c1 96)))
            (and (= n 2) (= c0 27)) (str "M-" (char c1))
            (= raw2 '(27 91 65)) "up"
            (= raw2 '(27 91 66)) "down"
            (= raw2 '(27 91 67)) "right"
            (= raw2 '(27 91 68)) "left"
            (= raw2 '(27 91 72)) "home"
            (= raw2 '(27 91 70)) "end"
            (= raw2 '(27 91 53 126)) "pgup"
            (= raw2 '(27 91 54 126)) "pgdn"
            (= raw2 '(27 91 50 126)) "ins"
            (= raw2 '(27 91 51 126)) "delete"
            (= raw2 '(27 79 81)) "f2"
            (= raw2 '(27 79 82)) "f3"
            (= raw2 '(27 79 83)) "f4"
            (= raw2 '(27 91 49 53 126)) "f5"
            (= raw2 '(27 91 49 55 126)) "f6"
            (= raw2 '(27 91 49 56 126)) "f7"
            (= raw2 '(27 91 49 57 126)) "f8"
            (= raw2 '(27 91 50 48 126)) "f9"
            (= raw2 '(27 91 50 52 126)) "f12"
            (= raw2 '(27 91 49 59 50 65)) "S-up"
            (= raw2 '(27 91 49 59 50 66)) "S-down"
            (= raw2 '(27 91 49 59 50 68)) "S-left"
            (= raw2 '(27 91 49 59 50 67)) "S-right"
            (= raw2 '(27 91 49 59 53 65)) "C-up"
            (= raw2 '(27 91 49 59 53 66)) "C-down"
            (= raw2 '(27 91 49 59 53 68)) "C-left"
            (= raw2 '(27 91 49 59 53 67)) "C-right"
            (= raw2 '(27 91 49 59 53 72)) "C-home"
            (= raw2 '(27 91 49 59 53 70)) "C-end"
            true (str (char c0))))))


(defn input-handler
  [fun]
  (set-raw-mode)
  (tty-print esc "0;37m" esc "2J")
  (tty-print esc "0;0H" esc "s")
  (future
    (let [r (java.io.BufferedReader. *in*)
          read-input (fn [] (raw2keyword
                              (let [input0 (.read r)]
                                (if (= input0 27)
                                  (loop [res (list input0)]
                                    (Thread/sleep 1)
                                    (if (not (.ready r))
                                      (reverse res)
                                      (recur (conj res (.read r)))))
                                 input0))))]
      (loop [input (read-input)]
        (when (not= input "q")
          (fun input)
          (recur (read-input)))))
    (shutdown-agents)
    (set-line-mode)))

