(ns liq2.modes.dired-mode
  (:require [clojure.string :as str]
            [liq2.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq2.buffer :as buffer]
            [liq2.util :as util]))

(defn load-content
  [buf folder]
  (-> buf
      buffer/clear
      (buffer/insert-string
        (str
          (util/resolve-path folder ".") "\n../\n./\n"
          (str/join "\n" (sort (map #(str (util/filename %) "/") (util/get-folders folder))))
          "\n"
          (str/join "\n" (sort (map #(str (util/filename %)) (util/get-files folder))))))
      buffer/beginning-of-buffer
      buffer/down))

(defn run
  []
  (let [f (or (buffer/get-filename (editor/get-current-buffer)) ".")
        folder (util/absolute (util/get-folder f))
        id (editor/get-buffer-id-by-name "*dired*")]
    (if id
      (switch-to-buffer id)
      (editor/new-buffer "" {:major-mode :dired-mode :name "*dired*"}))
    (apply-to-buffer #(load-content % folder))
    (editor/highlight-buffer)))

(defn open-file
  [path]
  (editor/new-buffer (or (util/read-file path) "") {:name path :filename path}))

(defn choose
  []
  (let [buf (editor/get-current-buffer)
        parent (buffer/get-line buf 1)
        f (buffer/get-line buf)
        path (str parent "/" f)]
    (when (> (buffer/get-row buf) 1)
      (if (util/folder? path)
        (do 
          (apply-to-buffer #(load-content % path))
          (editor/highlight-buffer))
        (open-file path)))))

(def mode
  {:insert {"esc" (fn [] (apply-to-buffer #(-> % (buffer/set-mode :normal) buffer/left)))}
   :normal {"q" editor/previous-buffer
            "\n" choose
            "h" #(apply-to-buffer buffer/left)
            "j" #(apply-to-buffer buffer/down)
            "k" #(apply-to-buffer buffer/up)
            "l" #(apply-to-buffer buffer/right)
            "n" #(apply-to-buffer buffer/search)
            "0" #(apply-to-buffer buffer/beginning-of-line)
            "$" #(apply-to-buffer buffer/end-of-line)
            "g" {"g" #(editor/apply-to-buffer buffer/beginning-of-buffer)}
            "G" #(apply-to-buffer buffer/end-of-buffer)
            "/" (fn [] (switch-to-buffer "*minibuffer*")
                       (apply-to-buffer #(-> % buffer/clear (buffer/insert-char \/))))
            ":" (fn [] (switch-to-buffer "*minibuffer*")
                       (apply-to-buffer #(-> % buffer/clear (buffer/insert-char \:))))}
    :visual {"esc" #(apply-to-buffer buffer/set-normal-mode)}
    :init run
    :syntax
     {:plain ; Context
       {:style :plain ; style
        :matchers {#"^/.*$" :root-file
                   #"[^/]+/$" :folder}}
      :root-file
       {:style :definition
        :matchers {#".|$" :plain}}

      :folder
       {:style :keyword
        :matchers {#".|$" :plain}}}})