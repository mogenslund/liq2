(ns liq2.extras.cool-stuff
  (:require [clojure.string :as str]
            [liq2.editor :as editor]
            [liq2.buffer :as buffer]
            [liq2.util :as util])
  (:import [java.net Socket]
           [java.io BufferedInputStream BufferedOutputStream
                    BufferedReader InputStreamReader]
           [java.nio.charset Charset]))

(defn output-snapshot
  [] 
  (let [id (editor/get-buffer-id-by-name "output-snapshot")]
    (if id
      (editor/switch-to-buffer id)
      (editor/new-buffer "" {:name "output-snapshot"}))
    (editor/apply-to-buffer
      #(-> %
           buffer/clear
           (buffer/insert-string
             (buffer/get-text (editor/get-buffer "output")))))))

;; clj -J-Dclojure.server.repl="{:port 5555 :accept clojure.core.server/repl}"
(def socket (atom nil))

(defn jack-in
  [port]
  (let [s (try (Socket. "localhost" (util/int-value port)) (catch Exception e nil))]
    (when s
      (reset! socket {:socket s
                      :in (BufferedInputStream. (.getInputStream s))
                      :out (BufferedOutputStream. (.getOutputStream s))
                      :reader (BufferedReader. (InputStreamReader. (BufferedInputStream. (.getInputStream s))))})
      (future
        (loop []
          (when @socket
            (editor/message (.readLine (@socket :reader))))
          (recur))))))

(defn jack-out
  []
  (when (and @socket (@socket :socket))
    (when (.isConnected (@socket :socket))
      (.close (@socket :socket)))
  (reset! socket nil)))

(defn send-to-repl
  [text]
  (when (and @socket (@socket :socket) (.isConnected (@socket :socket)))
    (.write (@socket :out) (.getBytes (str text "\n") (Charset/forName "UTF-8")))
    (.flush (@socket :out))))

(defn send-sexp-at-point-to-repl 
  [buf]
  (let [sexp (if (= (buf ::buffer/mode) :visual)
               (buffer/get-selected-text buf)
               (buffer/sexp-at-point buf))]
    (send-to-repl sexp)))

(defn load-cool-stuff
  []
  (swap! editor/state assoc-in [::editor/commands :jack-in] jack-in) 
  (swap! editor/state assoc-in [::editor/commands :jack-out] jack-out) 
  (editor/add-key-bindings :clojure-mode :normal
    {"C-o" output-snapshot
     "f5" #(send-sexp-at-point-to-repl (editor/current-buffer))
     "f6" jack-out})
  (editor/add-key-bindings :clojure-mode :visual
    {"f5" #(send-sexp-at-point-to-repl (editor/current-buffer))}))
