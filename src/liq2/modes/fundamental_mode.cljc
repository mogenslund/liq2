(ns liq2.modes.fundamental-mode
  (:require [clojure.string :as str]
            [liq2.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq2.buffer :as buffer :refer [delete-region shrink-region set-insert-mode]]
            [liq2.util :as util]))

(defn non-repeat-fun
  [fun]
  (when (not= (@editor/state ::repeat-counter) 0) (swap! editor/state assoc ::repeat-counter 0))
  (editor/apply-to-buffer fun))

(def mode
  {:commands {":ts" #(editor/message (str % " -- " (buffer/sexp-at-point (editor/current-buffer))))}
   :insert {"esc" (fn [] (apply-to-buffer #(buffer/left (buffer/set-normal-mode %))))
            "backspace" #(non-repeat-fun buffer/delete-backward)
            ;; Emacs
            "C-b" :left
            "C-n" :down
            "C-p" :up
            "C-f" :right
            "left" :left 
            "down" :down 
            "up" :up 
            "right" :right 
            "M-x" (fn [] (when (not= (@editor/state ::repeat-counter) 0) (swap! editor/state assoc ::repeat-counter 0))
                         (switch-to-buffer "*minibuffer*")
                         (non-repeat-fun #(-> % buffer/clear
                                                (buffer/insert-char \M)
                                                (buffer/insert-char \-)
                                                (buffer/insert-char \x)
                                                (buffer/insert-char \space))))}
   :normal {"esc" (when (not= (@editor/state ::repeat-counter) 0) (swap! editor/state assoc ::repeat-counter 0))
            "C- " #(((editor/get-mode :buffer-chooser-mode) :init))
            "C-b" :previous-regular-buffer
            "t" (fn [] (apply-to-buffer #(buffer/insert-string % "Just\nTesting")))
            "f2" editor/oldest-buffer
            "f3" #(non-repeat-fun buffer/debug-clear-undo)
            "0" :0 
            "1" :1 
            "2" :2 
            "3" :3 
            "4" :4 
            "5" :5 
            "6" :6 
            "7" :7
            "8" :8
            "9" :9 
            "%" :move-matching-paren
            "i" :set-insert-mode 
            "a" :insert-after-point
            "I" :insert-at-beginning-of-line
            "h" :left 
            "j" :down 
            "k" :up 
            "l" :right 
            "left" :left 
            "down" :down 
            "up" :up 
            "right" :right 
            "w" :word-forward
            "W" :word-forward-ws
            "b" :beginning-of-word
            "e" :end-of-word
            "E" :end-of-word-ws
            "^" :first-non-blank
            "$" :end-of-line
            "x" :delete-char
            "v" :set-visual-mode
            "n" :search
            "u" :undo
            "y" {"y" :copy-line
                 "%" :yank-filename
                 "i" {"w" :yank-inner-word
                      "(" :yank-inner-paren
                      "[" :yank-inner-bracket
                      "{" :yank-inner-brace
                      "\"" :yank-inner-quote}
                 "a" {"(" :yank-outer-paren
                      "[" :yank-outer-bracket
                      "{" :yank-outer-brace
                      "\"" :yank-outer-quote}}
            "p" :paste-clipboard
            "P" :paste-clipboard-here
            "g" {"g" :beginning-of-buffer
                 "i" :navigate-definitions
                 "f" :open-file-at-point
                 "J" :join-lines}
            "G" :end-of-buffer
            "z" {"t" :scroll-cursor-top 
                 "\n" :scroll-cursor-top}
            "d" {"d" :delete-line
                 "w" :delete-to-word
                 "e" :delete-to-end-of-word
                 "i" {"w" :delete-inner-word
                      "(" :delete-inner-paren
                      "[" :delete-inner-bracket
                      "{" :delete-inner-brace
                      "\"" :delete-inner-quote}
                 "a" {"(" :delete-outer-paren
                      "[" :delete-outer-bracket
                      "{" :delete-outer-brace
                      "\"" :delete-outer-quote}}
            "A" :insert-at-line-end
            "D" :delete-to-line-end
            "r" {:selfinsert (fn [buf c]
                               (when (not= (@editor/state ::repeat-counter) 0) (swap! editor/state assoc ::repeat-counter 0))
                               (buffer/set-char buf (first c)))}
            "c" {"p" {"p" :eval-sexp-at-point
                      "r" :raw-eval-sexp-at-point
                      "f" :evaluate-file-raw}
                 "i" {"w" :change-inner-word
                      "(" :change-inner-paren
                      "[" :change-inner-bracket
                      "{" :change-inner-brace
                      "\"" :change-inner-quote}
                 "a" {"(" :change-outer-paren
                      "[" :change-outer-bracket
                      "{" :change-outer-brace
                      "\"" :change-outer-quote}
                 "c" :change-line
                 "$" :change-eol
                 "e" :change-to-end-of-word
                 "w" :change-to-end-of-word}
            "C" :change-eol
            "/" (fn [] (when (not= (@editor/state ::repeat-counter) 0) (swap! editor/state assoc ::repeat-counter 0))
                       (switch-to-buffer "*minibuffer*")
                       (non-repeat-fun #(-> % buffer/clear (buffer/insert-char \/))))
            ":" (fn [] (when (not= (@editor/state ::repeat-counter) 0) (swap! editor/state assoc ::repeat-counter 0))
                       (switch-to-buffer "*minibuffer*")
                       (non-repeat-fun #(-> % buffer/clear (buffer/insert-char \:))))
            "Q" editor/record-macro
            "q" editor/run-macro
            "J" :join-lines-space
            "o" :append-line
            "O" :append-line-above}
    :visual {"esc" :set-normal-mode 
             "c" {"p" {"p" :eval-sexp-at-point
                       "r" :raw-eval-sexp-at-point}}
             "y" :copy-selection-to-clipboard
             "d" :delete 
             }})
