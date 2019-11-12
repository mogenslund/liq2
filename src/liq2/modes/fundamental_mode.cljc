(ns liq2.modes.fundamental-mode
  (:require [clojure.string :as str]
            [liq2.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq2.buffer :as buffer]
            #?(:cljs [cljs.js :refer [eval eval-str empty-state]])))

(defn tmp-eval
  []
  #?(:clj (let [res (load-string (buffer/get-selected-text (editor/get-current-buffer)))]
            (editor/switch-to-buffer (editor/get-buffer-id-by-name "*output*"))
            (editor/apply-to-buffer #(buffer/insert-string % (str res)))
            (editor/push-output)
            (Thread/sleep 10)
            (editor/previous-buffer))
     :cljs (do (set! cljs.js/*eval-fn* cljs.js/js-eval) (eval-str (empty-state) (buffer/get-selected-text (editor/get-current-buffer)) str))))

(defn tmp-get-text
  []
  (let [res (buffer/get-selected-text (editor/get-current-buffer))]
    (editor/switch-to-buffer (editor/get-buffer-id-by-name "*output*"))
    (editor/apply-to-buffer #(buffer/insert-string % (str res)))
    (editor/push-output)
    (Thread/sleep 10)
    (editor/previous-buffer)))

(def sample-code "(ns user.user (:require [liq2.editor :as editor] [liq2.buffer :as buffer])) (liq2.editor/apply-to-buffer liq2.buffer/end-of-line) :something")

(def mode
  {:insert {"esc" (fn [] (apply-to-buffer #(-> % (buffer/set-mode :normal) buffer/backward-char)))
            "backspace" (fn [] (apply-to-buffer #(if (> (buffer/get-col %) 1) (-> % buffer/backward-char buffer/delete-char) %)))}
   :normal {"esc" #(apply-to-buffer buffer/remove-selection)
            "t" (fn [] (apply-to-buffer #(buffer/insert-string % "Just\nTesting")))
            "f2" editor/oldest-buffer
            "i" (fn [] (apply-to-buffer #(buffer/set-mode % :insert)))
            "h" #(apply-to-buffer buffer/backward-char)
            "j" #(apply-to-buffer buffer/next-line)
            "k" #(apply-to-buffer buffer/previous-line)
            "l" #(apply-to-buffer buffer/forward-char)
            "0" #(apply-to-buffer buffer/beginning-of-line)
            "$" #(apply-to-buffer buffer/end-of-line)
            "x" #(apply-to-buffer buffer/delete-char)
            "v" #(apply-to-buffer buffer/set-selection)
            "g" {"g" #(editor/apply-to-buffer buffer/beginning-of-buffer)}
            "G" #(apply-to-buffer buffer/end-of-buffer)
            "c" {"p" {"p" tmp-eval
                      "t" tmp-get-text}}
            "/" (fn [] (switch-to-buffer "*minibuffer*")
                       (apply-to-buffer #(-> % buffer/clear (buffer/insert-char \/))))
            ":" (fn [] (switch-to-buffer "*minibuffer*")
                       (apply-to-buffer #(-> % buffer/clear (buffer/insert-char \:))))
            "o" #(apply-to-buffer buffer/append-line)}})