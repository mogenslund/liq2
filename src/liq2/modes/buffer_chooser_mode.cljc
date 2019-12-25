(ns liq2.modes.buffer-chooser-mode
  (:require [clojure.string :as str]
            [liq2.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq2.buffer :as buffer]
            [liq2.util :as util]))

(defn load-content
  [buf]
  (-> buf
      buffer/clear
      (buffer/insert-string (str/join "\n"
                              (map #(str (% ::buffer/name) (if (buffer/dirty? %) " [+]" "    "))
                                   (rest (editor/all-buffers)))))
      buffer/beginning-of-buffer))

(defn run
  []
  (if-let [id (editor/get-buffer-id-by-name "*buffer-chooser*")]
    (switch-to-buffer id)
    (editor/new-buffer "" {:major-mode :buffer-chooser-mode :name "*buffer-chooser*"}))
  (apply-to-buffer load-content))

(defn choose-buffer
  []
  (editor/previous-buffer (-> (editor/current-buffer) ::buffer/cursor ::buffer/row)))

(def mode
  {:insert {"esc" (fn [] (apply-to-buffer #(-> % (assoc ::buffer/mode :normal) buffer/left)))}
   :normal {"q" editor/previous-buffer
            "\n" choose-buffer
            "h" :left 
            "j" :down
            "k" :up
            "l" :right
            "C-b" :left
            "C-n" :down
            "C-p" :up
            "C-f" :right
            "left" :left 
            "down" :down 
            "up" :up 
            "right" :right
            "0" #(apply-to-buffer buffer/beginning-of-line)
            "$" #(apply-to-buffer buffer/end-of-line)
            "g" {"g" #(editor/apply-to-buffer buffer/beginning-of-buffer)}
            "G" #(apply-to-buffer buffer/end-of-buffer)
            "/" (fn [] (switch-to-buffer "*minibuffer*")
                       (apply-to-buffer #(-> % buffer/clear (buffer/insert-char \/))))
            ":" (fn [] (switch-to-buffer "*minibuffer*")
                       (apply-to-buffer #(-> % buffer/clear (buffer/insert-char \:))))}
    :visual {"esc" #(apply-to-buffer buffer/set-normal-mode)}
    :init run})
