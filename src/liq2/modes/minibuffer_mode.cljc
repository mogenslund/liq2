(ns liq2.modes.minibuffer-mode
  (:require [clojure.string :as str]
            [liq2.util :as util]
            #?(:clj [liq2.tools.shell :as s])
            [liq2.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq2.buffer :as buffer]))

(defn write-file
  []
  (let [buf (editor/get-current-buffer)]
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
   #?(:clj (let [f (or ((editor/get-current-buffer) ::buffer/filename) ".")
                 folder (util/absolute (util/get-folder f))]
             (editor/message (str "Running command: " text "\n") :view true)
             (future
               (doseq [output (s/cmdseq folder "/bin/sh" "-c" text)]
                 (editor/message output :append true)))))
   #?(:cljs (do)))

(defn execute
  []
  (let [content (buffer/get-text (editor/get-current-buffer))
        ;[command param] (str/split content #" " 2)
        ]
    (apply-to-buffer buffer/clear)
    (editor/paint-buffer)
    (editor/previous-buffer)
    (cond (<= (count content) 1) (do)
          (= content ":q") (editor/exit-program)
          (= content ":q!") (editor/force-exit-program)
          (= content ":bnext") (editor/oldest-buffer)
          (= content ":new") (editor/new-buffer "" {})
          (= content ":buffers") (((editor/get-mode :buffer-chooser-mode) :init)) 
          (= content ":w") (write-file) 
          (= content ":t") (editor/open-file "/home/sosdamgx/proj/liquid/src/dk/salza/liq/slider.clj")
          (= content ":bd") (editor/kill-buffer)
          (= content ":bd!") (editor/force-kill-buffer)
          (= content ":t1") (editor/highlight-buffer)
          (= content ":ts") (editor/message (buffer/sexp-at-point (editor/get-current-buffer)))
          (= content ":t2") (editor/message (buffer/get-word (editor/get-current-buffer)))
          (= content ":t3") (((editor/get-mode :typeahead-mode) :init) ["aaa" "bbb" "aabb" "ccc"] str buffer/insert-string) 
          (= content ":t4") (editor/message (pr-str (buffer/get-line (editor/get-current-buffer) 1)))
          (= content ":t5") (editor/message (pr-str (:liq2.buffer/lines (editor/get-current-buffer))))
          (= content ":t6") (((editor/get-mode :info-dialog-mode) :init) "This is the info dialog")
          (= content ":e .") (((editor/get-mode :dired-mode) :init))
          (re-matches #":! .*" content) (external-command (subs content 3))
          (re-matches #":e .*" content) (editor/open-file (subs content 3))
          (= (subs content 0 1) "/") (apply-to-buffer #(buffer/search % (subs content 1))))))

(def mode
  {:commands {":tt" (fn [t] (editor/message (buffer/sexp-at-point (editor/get-current-buffer))))}
   :insert {"esc" editor/previous-buffer
            "backspace" (fn [] (apply-to-buffer #(if (> (-> % ::buffer/cursor ::buffer/col %) 1) (-> % buffer/left buffer/delete-char) %)))
            "\n" execute}
   :normal {"esc" (fn [] (apply-to-buffer #(assoc % ::buffer/mode :insert)))
            "l" #(apply-to-buffer buffer/right)}})
