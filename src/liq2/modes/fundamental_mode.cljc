(ns liq2.modes.fundamental-mode
  (:require [clojure.string :as str]
            [liq2.editor :as editor]
            [liq2.buffer :as buffer]
            #?(:cljs [cljs.js :refer [eval eval-str empty-state]])))

(defn tmp-eval
  [s]
  #?(:clj (let [res (load-string s)]
            (editor/switch-to-buffer (editor/get-buffer-id-by-name "*output*"))
            (editor/apply-to-buffer #(buffer/insert-string % (str res)))
            (editor/push-output)
            (Thread/sleep 10)
            (editor/previous-buffer))
     :cljs (do (set! cljs.js/*eval-fn* cljs.js/js-eval) (eval-str (empty-state) s str))))

(def sample-code "(ns user.user (:require [liq2.editor :as editor] [liq2.buffer :as buffer])) (liq2.editor/apply-to-buffer liq2.buffer/end-of-line) :something")

(def mode
  {:insert {"esc" {:function #(-> % (buffer/set-mode :normal) buffer/backward-char) :type :buffer}
            "backspace" {:function #(if (> (buffer/get-col %) 1) (-> % buffer/backward-char buffer/delete-char) %) :type :buffer}}
   :normal {"esc" {:function buffer/remove-selection :type :buffer}
            "t" {:function #(buffer/insert-string % "Just\nTesting") :type :buffer}
            "f2" editor/oldest-buffer
            "i" {:function #(buffer/set-mode % :insert) :type :buffer}
            "h" {:function buffer/backward-char :type :buffer}
            "j" {:function buffer/next-line :type :buffer}
            "k" {:function buffer/previous-line :type :buffer}
            "l" {:function buffer/forward-char :type :buffer}
            "0" {:function buffer/beginning-of-line :type :buffer}
            "$" {:function buffer/end-of-line :type :buffer}
            "x" {:function buffer/delete-char :type :buffer}
            "v" {:function buffer/set-selection :type :buffer}
            "g" {:keymap {"g" {:function buffer/beginning-of-buffer :type :buffer}}}
            "G" {:function buffer/end-of-buffer :type :buffer}
            "c" {:keymap {"p" {:keymap {"p" #(tmp-eval sample-code)}}}}
            "/" (fn [] (editor/switch-to-buffer (editor/get-buffer-id-by-name "*minibuffer*")) (editor/apply-to-buffer #(-> % buffer/clear (buffer/insert-char \/))))
            ":" (fn [] (editor/switch-to-buffer (editor/get-buffer-id-by-name "*minibuffer*")) (editor/apply-to-buffer #(-> % buffer/clear (buffer/insert-char \:))))
            "o" {:function buffer/append-line :type :buffer}}})