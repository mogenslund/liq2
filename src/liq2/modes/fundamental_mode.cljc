(ns liq2.modes.fundamental-mode
  (:require [clojure.string :as str]
            [liq2.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq2.buffer :as buffer :refer [delete-region shrink-region set-insert-mode]]
            [liq2.util :as util]))

(def repeat-counter (atom "0"))

(defn reset-repeat-counter [] (reset! repeat-counter "0"))

(defn repeat-fun
  [fun]
  (let [r (max (min (Integer/parseInt @repeat-counter) 99) 1)]
    (reset-repeat-counter)
    (editor/apply-to-buffer #(fun % r))))

(defn non-repeat-fun
  [fun]
  (reset-repeat-counter)
  (editor/apply-to-buffer fun))

(defn tmp-eval
  []
  (let [res (util/eval-safe (buffer/get-selected-text (editor/get-current-buffer)))]
    (editor/apply-to-buffer "*output*" #(-> % buffer/clear (buffer/insert-string (str res))))
    (editor/paint-buffer (get-buffer "*output*"))))

(defn get-namespace
  [buf]
  (let [content (buffer/get-line buf 1)]
    (re-find #"(?<=\(ns )[-a-z0-9\\.]+" content))) ;)

(defn eval-sexp-at-point-old
  [buf]
  (reset-repeat-counter) 
  (let [namespace (or (get-namespace buf) "user")]
    (util/eval-safe
      (str
        "(do (ns " namespace ") (in-ns '"
        namespace
        ") "
        (buffer/sexp-at-point buf) ")"))))

(defn eval-sexp-at-point
  [buf]
  (reset-repeat-counter) 
  (binding [*print-length* 200]
    (let [sexp (buffer/sexp-at-point buf)
          pr-str-str (fn [x] (if (string? x) x (pr-str x)))
          namespace (or (get-namespace buf) "user")]
      (editor/message "" :view true)
      (future
        (with-redefs [println (fn [& args] (editor/message (str/join " " args) :append true))]
          (try
            (println (pr-str-str
              (load-string
                (str
                  "(do (ns " namespace ") (in-ns '"
                  namespace
                  ") " sexp ")"))))
            (catch Exception e (println (util/pretty-exception e)))))))))


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
  (reset-repeat-counter) 
  (let [buf (editor/get-current-buffer)
        part (buffer/get-word buf)
        buffer-file (or (buffer/get-filename buf) (buffer/get-filename (editor/get-buffer (editor/previous-regular-buffer-id))))
        alternative-parent (if buffer-file (util/get-folder buffer-file) ".")
        filepath (util/resolve-path part alternative-parent)]
    (editor/open-file filepath)))

(defn evaluate-file-raw
  "Evaluate a given file raw, without using
  with-out-str or other injected functionality.
  If no filepath is supplied the path connected
  to the current buffer will be used."
  ([filepath]
    (try (editor/message (load-file filepath))
      (catch Exception e (editor/message (util/pretty-exception e)))))
  ([] (when-let [filepath (buffer/get-filename (editor/get-current-buffer))] (evaluate-file-raw filepath))))

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
  (reset-repeat-counter)
  (if (util/clipboard-line?)
    (apply-to-buffer
      #(-> %
           buffer/append-line
           (buffer/insert-string (util/clipboard-content))
           buffer/beginning-of-line
           buffer/set-normal-mode))
    (apply-to-buffer
      #(-> %
           buffer/set-insert-mode
           buffer/right
           (buffer/insert-string (util/clipboard-content))
           buffer/left
           buffer/set-normal-mode))))

(defn paste-clipboard-here
  []
  (reset-repeat-counter)
  (if (util/clipboard-line?)
    (apply-to-buffer
      #(-> %
           buffer/beginning-of-line
           (buffer/insert-string (str (util/clipboard-content) "\n"))
           buffer/up
           buffer/beginning-of-line
           buffer/set-normal-mode))
    (apply-to-buffer
      #(-> %
           buffer/set-insert-mode
           (buffer/insert-string (util/clipboard-content))
           buffer/left
           buffer/set-normal-mode))))

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

(defn cut-region
  [buf r]
  (if r
    (let [text (buffer/get-text buf r)]
      (util/set-clipboard-content text false)
      (buffer/delete-region buf r))
    buf))

(defn yank-region
  [buf r]
  (if r
    (let [text (buffer/get-text buf r)]
      (util/set-clipboard-content text false)
      (buffer/set-point buf (first r)))
    buf))

(defn yank-filename
  []
  (when-let [f (buffer/get-filename (editor/get-current-buffer))]
    (util/set-clipboard-content f false)))

(def sample-code "(ns user.user (:require [liq2.editor :as editor] [liq2.buffer :as buffer])) (liq2.editor/apply-to-buffer liq2.buffer/end-of-line) :something")

(def mode
  {:insert {"esc" (fn [] (apply-to-buffer #(buffer/left (buffer/set-normal-mode %))))
            "backspace" #(non-repeat-fun buffer/delete-backward)}
   :normal {"esc" #(reset! repeat-counter "0") 
            "C- " #(((editor/get-mode :buffer-chooser-mode) :init))
            "t" (fn [] (apply-to-buffer #(buffer/insert-string % "Just\nTesting")))
            "f2" editor/oldest-buffer
            "f3" #(non-repeat-fun buffer/debug-clear-undo)
            "0" #(if (= @repeat-counter "0") (non-repeat-fun buffer/beginning-of-line) (swap! repeat-counter str "0"))
            "1" #(swap! repeat-counter str "1")
            "2" #(swap! repeat-counter str "2")
            "3" #(swap! repeat-counter str "3")
            "4" #(swap! repeat-counter str "4")
            "5" #(swap! repeat-counter str "5")
            "6" #(swap! repeat-counter str "6")
            "7" #(swap! repeat-counter str "7")
            "8" #(swap! repeat-counter str "8")
            "9" #(swap! repeat-counter str "9")
            "%" #(non-repeat-fun buffer/move-matching-paren)
            "i" #(non-repeat-fun buffer/set-insert-mode)
            "a" (fn [] (non-repeat-fun #(-> % buffer/set-insert-mode buffer/right)))
            "h" #(repeat-fun buffer/left)
            "j" #(repeat-fun buffer/down)
            "k" #(repeat-fun buffer/up)
            "l" #(repeat-fun buffer/right)
            "w" #(repeat-fun buffer/word-forward)
            "b" #(repeat-fun buffer/beginning-of-word)
            "e" #(repeat-fun buffer/end-of-word)
            "$" #(non-repeat-fun buffer/end-of-line)
            "x" #(repeat-fun buffer/delete-char)
            "v" #(non-repeat-fun buffer/set-visual-mode)
            "n" #(non-repeat-fun buffer/search)
            "u" #(non-repeat-fun buffer/undo)
            "y" {"y" copy-line
                 "%" yank-filename
                 "i" {"(" (fn [] (non-repeat-fun #(yank-region % (shrink-region % (buffer/paren-region %)))))
                      "[" (fn [] (non-repeat-fun #(yank-region % (shrink-region % (buffer/bracket-region %)))))
                      "{" (fn [] (non-repeat-fun #(yank-region % (shrink-region % (buffer/brace-region %)))))}
                 "a" {"(" (fn [] (non-repeat-fun #(yank-region % (buffer/paren-region %))))
                      "[" (fn [] (non-repeat-fun #(yank-region % (buffer/bracket-region %))))
                      "{" (fn [] (non-repeat-fun #(yank-region % (buffer/brace-region %))))}}
            "p" paste-clipboard
            "P" paste-clipboard-here
            "g" {"g" #(non-repeat-fun buffer/beginning-of-buffer)
                 "f" open-file-at-point}
            "G" #(non-repeat-fun buffer/end-of-buffer)
            "d" {"d" #(non-repeat-fun delete-line)
                 "i" {"(" (fn [] (non-repeat-fun #(cut-region % (shrink-region % (buffer/paren-region %)))))
                      "[" (fn [] (non-repeat-fun #(cut-region % (shrink-region % (buffer/bracket-region %)))))
                      "{" (fn [] (non-repeat-fun #(cut-region % (shrink-region % (buffer/brace-region %)))))}
                 "a" {"(" (fn [] (non-repeat-fun #(cut-region % (buffer/paren-region %))))
                      "[" (fn [] (non-repeat-fun #(cut-region % (buffer/bracket-region %))))
                      "{" (fn [] (non-repeat-fun #(cut-region % (buffer/brace-region %))))}}
            "A" #(non-repeat-fun buffer/insert-at-line-end)
            "D" #(non-repeat-fun buffer/delete-to-line-end)
            "r" {:selfinsert (fn [buf c] (reset-repeat-counter) (buffer/set-char buf (first c)))}
            "c" {"p" {"p" #(eval-sexp-at-point (editor/get-current-buffer))
                      "t" tmp-print-buffer
                      "f" evaluate-file-raw}
                 "i" {"w" #(non-repeat-fun
                            (fn [buf]
                              (-> buf
                                  buffer/right
                                  buffer/beginning-of-word
                                  buffer/set-visual-mode
                                  buffer/end-of-word
                                  buffer/delete
                                  buffer/set-insert-mode)))
                      "(" (fn [] (non-repeat-fun #(set-insert-mode (delete-region % (shrink-region % (buffer/paren-region %))))))
                      "[" (fn [] (non-repeat-fun #(set-insert-mode (delete-region % (shrink-region % (buffer/bracket-region %))))))
                      "{" (fn [] (non-repeat-fun #(set-insert-mode (delete-region % (shrink-region % (buffer/brace-region %))))))}}
            "/" (fn [] (reset-repeat-counter)
                       (switch-to-buffer "*minibuffer*")
                       (non-repeat-fun #(-> % buffer/clear (buffer/insert-char \/))))
;            "-" (fn [] (reset-repeat-counter)
;                       (switch-to-buffer "*minibuffer*")
;                       (non-repeat-fun #(-> % buffer/clear (buffer/insert-char \/))))
;            "æ" (fn [] (reset-repeat-counter)
;                       (switch-to-buffer "*minibuffer*")
;                       (non-repeat-fun #(-> % buffer/clear (buffer/insert-char \:))))
            ":" (fn [] (reset-repeat-counter)
                       (switch-to-buffer "*minibuffer*")
                       (non-repeat-fun #(-> % buffer/clear (buffer/insert-char \:))))
            "Q" editor/record-macro
            "q" editor/run-macro
            "o" #(non-repeat-fun buffer/append-line)}
    :visual {"esc" #(non-repeat-fun buffer/set-normal-mode)
             "c" {"p" {"p" tmp-eval}}
             "y" #(apply-to-buffer copy-selection-to-clipboard)
             "d" #(apply-to-buffer delete)
             }})
