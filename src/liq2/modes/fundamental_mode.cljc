(ns liq2.modes.fundamental-mode
  (:require [clojure.string :as str]
            [liq2.editor :as editor]
            [liq2.buffer :as buffer]
            #?(:cljs [cljs.js :refer [eval eval-str empty-state]])))

(defn tmp-eval
  [s]
  #?(:clj (load-string (str "(println " s ")"))
     :cljs (do (set! cljs.js/*eval-fn* cljs.js/js-eval) (eval-str (empty-state) s println))))

(def mode
  {:insert {"esc" {:function #(-> % (buffer/set-mode :normal) buffer/backward-char) :type :buffer}}
   :normal {"i" {:function #(buffer/set-mode % :insert) :type :buffer}
            "h" {:function buffer/backward-char :type :buffer}
            "j" {:function buffer/next-line :type :buffer}
            "k" {:function buffer/previous-line :type :buffer}
            "l" {:function buffer/forward-char :type :buffer}
            "0" {:function buffer/beginning-of-line :type :buffer}
            "$" {:function buffer/end-of-line :type :buffer}
            "x" {:function buffer/delete-char :type :buffer}
            "g" {:keymap {"g" {:function buffer/beginning-of-buffer :type :buffer}}}
            "G" {:function buffer/end-of-buffer :type :buffer}
            "c" {:keymap {"p" {:keymap {"p" #(tmp-eval "(+ 1 2 3)")}}}}
            "/" (fn [] (editor/switch-to-buffer (editor/get-buffer-id-by-name "-minibuffer-")) (editor/apply-to-buffer #(-> % buffer/clear (buffer/insert-char \/))))
            ":" (fn [] (editor/previous-buffer) (editor/apply-to-buffer #(-> % buffer/clear (buffer/insert-char \:))))
            "o" {:function buffer/append-line :type :buffer}}})