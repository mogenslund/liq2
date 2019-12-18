(ns liq2.modes.minibuffer-mode
  (:require [clojure.string :as str]
            [liq2.util :as util]
            #?(:clj [liq2.tools.shell :as s])
            [liq2.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq2.buffer :as buffer]))

(defn write-file
  []
  (let [buf (editor/current-buffer)]
    (when-let [f (buf ::buffer/filename)]
      (util/write-file f (buffer/get-text buf)))
    (apply-to-buffer #(buffer/set-dirty % false))))

(defn split-args
  "Not needed"
  [text]
  (filter #(not= % "")
    (loop [s text arg "" res [] isstr false]
      (let [c (first s)]
        (cond (nil? c) (conj res arg)
              (and isstr (= c \")) (recur (rest s) "" (conj res arg) false)  
              (= c \") (recur (rest s) "" (conj res arg) true) 
              (and (not isstr) (= c \space)) (recur (rest s) "" (conj res arg) false)
              true (recur (rest s) (str arg c) res isstr))))))

(defn external-command
  [text]
   #?(:clj (let [f (or ((editor/current-buffer) ::buffer/filename) ".")
                 folder (util/absolute (util/get-folder f))]
             (editor/message (str "Running command: " text "\n") :view true)
             (future
               (doseq [output (s/cmdseq folder "/bin/sh" "-c" text)]
                 (editor/message output :append true)))))
   #?(:cljs (do)))

(defn e-cmd
  [t]
  (if (= t ".")
    (((editor/get-mode :dired-mode) :init))
    (editor/open-file t)))

(swap! editor/state assoc ::commands
  [[#"^:q$" #(editor/exit-program)]
   [#"^:q!$" #(editor/force-exit-program)]
   [#"^:bnext$" #(editor/oldest-buffer)]
   [#"^:new$" #(editor/new-buffer "" {})]
   [#"^:buffers$" #(((editor/get-mode :buffer-chooser-mode) :init))]
   [#"^:w$" #(write-file)]
   [#"^:wq$" #(do (write-file) (editor/exit-program))]
   [#"^:t$" #(editor/open-file "/home/sosdamgx/proj/liquid/src/dk/salza/liq/slider.clj")]
   [#"^:bd$" #(editor/kill-buffer)]
   [#"^:bd!$" #(editor/force-kill-buffer)]
   [#"^:t1$" #(editor/highlight-buffer)]
   [#"^:ts$" #(editor/message (buffer/sexp-at-point (editor/current-buffer)))]
   [#"^:t2$" #(editor/message (buffer/get-word (editor/current-buffer)))]
   [#"^:t3$" #(((editor/get-mode :typeahead-mode) :init)
                 ["aaa" "bbb" "aabb" "ccc"]
                 str
                 (fn [res]
                   (editor/previous-buffer)
                   (editor/apply-to-buffer (fn [buf] (buffer/insert-string buf res)))))]
   [#"^:t4$" #(editor/message (pr-str (buffer/get-line (editor/current-buffer) 1)))]
   [#"^:t5$" #(editor/message (pr-str (:liq2.buffer/lines (editor/current-buffer))))]
   [#"^:t6$" #(((editor/get-mode :info-dialog-mode) :init) "This is the info dialog")]
   [#"^:! (.*)" #(external-command %)]
   [#"^:e (.*)" e-cmd]
   [#"^/(.*)" (fn [t] (apply-to-buffer #(buffer/search % t)))]])

(defn resolve-and-execute
  [content]
  (let [res (first (filter first (map #(vector (re-matches (first %) content) (second %)) (@editor/state ::commands))))]
    (when res
      (if (vector? (first res))
        (apply (second res) (rest (first res)))
        ((second res))))))

(defn execute
  []
  (let [content (buffer/get-text (editor/current-buffer))
        ;[command param] (str/split content #" " 2)
        ]
    (apply-to-buffer buffer/clear)
    (editor/paint-buffer)
    (editor/previous-buffer)
    (when (> (count content) 0)
      (resolve-and-execute content))))

(def mode
  {:commands {":tt" (fn [t] (editor/message (buffer/sexp-at-point (editor/current-buffer))))}
   :insert {"esc" editor/previous-buffer
            "backspace" (fn [] (apply-to-buffer #(if (> (-> % ::buffer/cursor ::buffer/col) 1) (-> % buffer/left buffer/delete-char) %)))
            "\n" execute}
   :normal {"esc" (fn [] (apply-to-buffer #(assoc % ::buffer/mode :insert)))
            "l" #(apply-to-buffer buffer/right)}})
