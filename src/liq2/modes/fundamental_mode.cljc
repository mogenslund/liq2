(ns liq2.modes.fundamental-mode
  (:require [clojure.string :as str]
            [liq2.editor :as editor]
            [liq2.buffer :as buffer]
            #?(:cljs [cljs.js :refer [eval eval-str empty-state]])))

(defn tmp-eval
  [s]
  #?(:clj (load-string s)
     :cljs (do (set! cljs.js/*eval-fn* cljs.js/js-eval) (eval-str (empty-state) s str))))

(def sample-code "(ns user.user (:require [liq2.editor :as editor] [liq2.buffer :as buffer])) (liq2.editor/apply-to-buffer liq2.buffer/end-of-line)")

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
            "c" {:keymap {"p" {:keymap {"p" #(tmp-eval sample-code)}}}}
            "/" (fn [] (editor/switch-to-buffer (editor/get-buffer-id-by-name "-minibuffer-")) (editor/apply-to-buffer #(-> % buffer/clear (buffer/insert-char \/))))
            ":" (fn [] (editor/switch-to-buffer (editor/get-buffer-id-by-name "-minibuffer-")) (editor/apply-to-buffer #(-> % buffer/clear (buffer/insert-char \:))))
            "o" {:function buffer/append-line :type :buffer}}})