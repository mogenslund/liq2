(ns liq2.extras.cool-stuff
  (:require [clojure.string :as str]
            [liq2.editor :as editor]
            [liq2.buffer :as buffer])
  (:import [java.net Socket]
           [java.io BufferedInputStream BufferedOutputStream
                    BufferedReader InputStreamReader]
           [java.nio.charset Charset]))

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

;; clj -J-Dclojure.server.repl="{:port 5555 :accept clojure.core.server/repl}"
(def socket (atom nil))

(defn jack-in
  [port]
  (let [s (Socket. "localhost" port)]
    (reset! socket {:socket s
                    :in (BufferedInputStream. (.getInputStream s))
                    :out (BufferedOutputStream. (.getOutputStream s))
                    :reader (BufferedReader. (InputStreamReader. (BufferedInputStream. (.getInputStream s))))})
    (future
      (loop []
        (when @socket
          (editor/message (.readLine (@socket :reader))))
        (recur)))))

(defn jack-out
  []
  (.close @socket)
  (reset! @socket nil))

(defn send-to-repl
  [text]
  (when (.isConnected (@socket :socket))
    (.write (@socket :out) (.getBytes (str text "\n") (Charset/forName "UTF-8")))
    (.flush (@socket :out))))

(defn send-sexp-at-point-to-repl 
  [buf]
  (when (not @socket) (jack-in 5555))
  (let [sexp (if (= (buffer/get-mode buf) :visuel)
               (buffer/get-selected-text buf)
               (buffer/sexp-at-point buf))]
    (send-to-repl sexp)))


(defn load-cool-stuff
  []
  (editor/add-key-bindings :clojure-mode :normal
    {"C-o" output-snapshot
     "f5" #(send-sexp-at-point-to-repl (editor/get-current-buffer))
     "f6" jack-out})
  (editor/add-key-bindings :clojure-mode :visual
    {"f5" #(send-sexp-at-point-to-repl (editor/get-current-buffer))}))