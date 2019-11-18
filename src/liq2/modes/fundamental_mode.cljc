(ns liq2.modes.fundamental-mode
  (:require [clojure.string :as str]
            [liq2.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq2.buffer :as buffer]
            [liq2.util :as util]))

(defn set-output
  [s]
  (editor/apply-to-buffer "*output*" #(-> % buffer/clear (buffer/insert-string s)))
  (editor/paint-buffer (get-buffer "*output*")))

(defn tmp-eval
  []
  (let [res (util/eval-safe (buffer/get-selected-text (editor/get-current-buffer)))]
    (editor/apply-to-buffer "*output*" #(-> % buffer/clear (buffer/insert-string (str res))))
    (editor/paint-buffer (get-buffer "*output*"))))

(defn tmp-eval-sexp-at-point
  []
  (let [res (util/eval-safe (buffer/sexp-at-point (editor/get-current-buffer)))]
    (editor/apply-to-buffer "*output*" #(-> % buffer/clear (buffer/insert-string (str res))))
    (editor/paint-buffer (get-buffer "*output*"))))

(defn tmp-get-text
  []
  (let [res (buffer/get-selected-text (editor/get-current-buffer))]
    (editor/apply-to-buffer "*output*" #(-> % buffer/clear (buffer/insert-string (str res))))
    (editor/paint-buffer (get-buffer "*output*"))))

(defn tmp-print-buffer
  []
  (let [res (pr-str (editor/get-current-buffer))]
    (editor/apply-to-buffer "*output*" #(-> % buffer/clear (buffer/insert-string (str res))))
    (editor/paint-buffer (get-buffer "*output*"))))

(defn open-file-at-point
  []
  (let [buf (editor/get-current-buffer)
        part (buffer/get-word buf)
        buffer-file (buffer/get-filename buf)
        alternative-parent (if buffer-file (util/get-folder buffer-file) ".")
        filepath (util/resolve-path part alternative-parent)]
    (editor/new-buffer (or (util/read-file filepath) "") {:name filepath :filename filepath})))


(def sample-code "(ns user.user (:require [liq2.editor :as editor] [liq2.buffer :as buffer])) (liq2.editor/apply-to-buffer liq2.buffer/end-of-line) :something")

(def mode
  {:insert {"esc" (fn [] (apply-to-buffer #(-> % (buffer/set-mode :normal) buffer/backward-char)))
            "backspace" (fn [] (apply-to-buffer #(if (> (buffer/get-col %) 1) (-> % buffer/backward-char buffer/delete-char) %)))}
   :normal {"C- " #(((editor/get-mode :buffer-chooser-mode) :init))
            "t" (fn [] (apply-to-buffer #(buffer/insert-string % "Just\nTesting")))
            "f2" editor/oldest-buffer
            "i" #(apply-to-buffer buffer/set-insert-mode)
            "h" #(apply-to-buffer buffer/backward-char)
            "j" #(apply-to-buffer buffer/next-line)
            "k" #(apply-to-buffer buffer/previous-line)
            "l" #(apply-to-buffer buffer/forward-char)
            "0" #(apply-to-buffer buffer/beginning-of-line)
            "$" #(apply-to-buffer buffer/end-of-line)
            "x" #(apply-to-buffer buffer/delete-char)
            "v" #(apply-to-buffer buffer/set-visual-mode)
            "g" {"g" #(editor/apply-to-buffer buffer/beginning-of-buffer)
                 "f" open-file-at-point}
            "G" #(apply-to-buffer buffer/end-of-buffer)
            "A" #(apply-to-buffer buffer/insert-at-line-end)
            "c" {"p" {"p" tmp-eval-sexp-at-point
                      "t" tmp-print-buffer}}
            "/" (fn [] (switch-to-buffer "*minibuffer*")
                       (apply-to-buffer #(-> % buffer/clear (buffer/insert-char \/))))
            ":" (fn [] (switch-to-buffer "*minibuffer*")
                       (apply-to-buffer #(-> % buffer/clear (buffer/insert-char \:))))
            "o" #(apply-to-buffer buffer/append-line)}
    :visual {"esc" #(apply-to-buffer buffer/set-normal-mode)
            "c" {"p" {"p" tmp-eval}}
             ;"y" editor/yank
             }})