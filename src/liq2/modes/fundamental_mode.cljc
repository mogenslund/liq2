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

(defn copy-selection-to-clipboard
  [buf]
  (let [p (buffer/get-selection buf)
        text (buffer/get-selected-text buf)]
    (if p
      (do
        (util/set-clipboard-content text false)
        (-> buf
            buffer/set-normal-mode
            (buffer/set-point p)
            buffer/update-mem-col))
      buf)))

(defn paste-clipboard
  []
  (if (util/clipboard-line?)
    (apply-to-buffer
      #(-> %
           buffer/append-line
           (buffer/insert-string (util/clipboard-content))
           buffer/beginning-of-line
           buffer/set-normal-mode))
    (apply-to-buffer #(buffer/insert-string % (util/clipboard-content)))))

(defn delete-line
  [buf]
  (let [text (buffer/get-line buf)]
    (util/set-clipboard-content text true)
    (buffer/delete-line buf)))

(defn copy-line
  []
  (let [text (buffer/get-line (editor/get-current-buffer))]
    (util/set-clipboard-content text true)))

(defn delete
  [buf]
  (let [text (buffer/get-selected-text buf)]
    (util/set-clipboard-content text false)
    (buffer/delete buf)))


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
            "y" {"y" copy-line}
            "p" paste-clipboard
            "g" {"g" #(editor/apply-to-buffer buffer/beginning-of-buffer)
                 "f" open-file-at-point}
            "G" #(apply-to-buffer buffer/end-of-buffer)
            "d" {"d" #(apply-to-buffer delete-line)}
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
             "y" #(apply-to-buffer copy-selection-to-clipboard)
             "d" #(apply-to-buffer delete)
             }})