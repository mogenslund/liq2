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
                              (map #(str (buffer/get-name %)) (rest (editor/all-buffers)))))
      buffer/beginning-of-buffer))

(defn run
  []
  (if-let [id (editor/get-buffer-id-by-name "*buffer-chooser*")]
    (switch-to-buffer id)
    (editor/new-buffer "" {:major-mode :buffer-chooser-mode :name "*buffer-chooser*"}))
  (apply-to-buffer load-content))

(defn choose-buffer
  []
  (editor/previous-buffer (buffer/get-row (editor/get-current-buffer))))

(def mode
  {:insert {"esc" (fn [] (apply-to-buffer #(-> % (buffer/set-mode :normal) buffer/left)))}
   :normal {"q" editor/previous-buffer
            "\n" choose-buffer
            "h" #(apply-to-buffer buffer/left)
            "j" #(apply-to-buffer buffer/down)
            "k" #(apply-to-buffer buffer/up)
            "l" #(apply-to-buffer buffer/right)
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