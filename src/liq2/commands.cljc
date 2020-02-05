(ns liq2.commands
  (:require [clojure.string :as str]
            [liq2.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq2.buffer :as buffer :refer [delete-region shrink-region set-insert-mode]]
            #?(:clj [liq2.tools.shell :as s])
            [liq2.util :as util]))

(swap! editor/state assoc ::repeat-counter 0)

(defn repeat-fun
  [fun args]
  (let [r (max (min (@editor/state ::repeat-counter) 999) 1)
        n (when (not (empty? args)) (util/int-value (first args)))]
    (when (not= (@editor/state ::repeat-counter) 0) (swap! editor/state assoc ::repeat-counter 0))
    (editor/apply-to-buffer #(fun % (or n r)))))

(defn non-repeat-fun
  [fun]
  (when (not= (@editor/state ::repeat-counter) 0) (swap! editor/state assoc ::repeat-counter 0))
  (editor/apply-to-buffer fun))

(defn open-file-at-point
  []
  (when (not= (@editor/state ::repeat-counter) 0) (swap! editor/state assoc ::repeat-counter 0))
  (let [buf (editor/current-buffer)
        part (re-find #"[^:]+" (buffer/get-word buf))
        buffer-file (or (buf ::buffer/filename) ((editor/get-buffer (editor/previous-regular-buffer-id)) ::buffer/filename))
        alternative-parent (if buffer-file (util/get-folder buffer-file) ".")
        filepath (util/resolve-path part alternative-parent)]
    (editor/open-file filepath)))

(defn write-file
  []
  (let [buf (editor/current-buffer)]
    (when-let [f (buf ::buffer/filename)]
      (util/write-file f (buffer/get-text buf)))
    (apply-to-buffer #(buffer/set-dirty % false))))

(defn external-command
  [text]
   #?(:clj (let [f (or ((editor/current-buffer) ::buffer/filename) ".")
                 folder (util/absolute (util/get-folder f))
                 text1 (str/replace text #"%" f)]
             (editor/message (str "Running command: " text1 "\n") :view true)
             (future
               (doseq [output (s/cmdseq folder "/bin/sh" "-c" text1)]
                 (editor/message output :append true)))))
   #?(:cljs (do)))

(defn e-cmd
  [t]
  (cond (= t ".") (((editor/get-mode :dired-mode) :init))
        (util/folder? t) (((editor/get-mode :dired-mode) :init) t)
        true (editor/open-file t)))

(defn copy-selection-to-clipboard
  [buf]
  (let [p (buffer/get-selection buf)
        text (buffer/get-selected-text buf)]
    (if p
      (do
        (util/set-clipboard-content text false)
        (-> buf
            buffer/set-normal-mode
            (assoc ::buffer/cursor p)
            buffer/update-mem-col))
      buf)))

(defn paste-clipboard
  []
  (when (not= (@editor/state ::repeat-counter) 0) (swap! editor/state assoc ::repeat-counter 0))
  (let [text (util/clipboard-content)] 
    (if (util/clipboard-line?)
      (apply-to-buffer
        #(-> %
             buffer/append-line
             ;buffer/end-of-line
             (buffer/insert-string text)
             buffer/beginning-of-line
             buffer/set-normal-mode))
      (apply-to-buffer
        #(-> %
             buffer/set-insert-mode
             buffer/right
             (buffer/insert-string text)
             (buffer/right (dec (count text)))
             buffer/set-normal-mode)))))

(defn paste-clipboard-here
  []
  (when (not= (@editor/state ::repeat-counter) 0) (swap! editor/state assoc ::repeat-counter 0))
  (let [text (util/clipboard-content)] 
    (if (util/clipboard-line?)
      (apply-to-buffer
        #(-> %
             buffer/beginning-of-line
             (buffer/insert-string (str text "\n"))
             buffer/up
             buffer/beginning-of-line
             buffer/set-normal-mode))
      (apply-to-buffer
        #(-> %
             buffer/set-insert-mode
             (buffer/insert-string text)
             (buffer/right (dec (count text)))
             buffer/set-normal-mode)))))

(defn delete-line
  [buf]
  (let [text (buffer/get-line buf)]
    (util/set-clipboard-content text true)
    (buffer/delete-line buf)))

(defn copy-line
  []
  (let [text (buffer/get-line (editor/current-buffer))]
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
      (assoc buf ::buffer/cursor (first r)))
    buf))

(defn yank-filename
  []
  (when-let [f ((editor/current-buffer) ::buffer/filename)]
    (util/set-clipboard-content f false)))

(defn typeahead-defs
  [buf]
  (let [headlines (filter #(re-find #"^\(def|^#" (second %))
                          (map #(vector % (buffer/get-line buf %))
                               (range 1 (inc (buffer/line-count buf)))))]
    (((editor/get-mode :typeahead-mode) :init) headlines 
                                               second
                                               (fn [res]
                                                 (editor/previous-buffer)
                                                 (apply-to-buffer #(-> %
                                                                       (assoc ::buffer/cursor {::buffer/row (first res) ::buffer/col 1})
                                                                       (assoc ::buffer/tow {::buffer/row (first res) ::buffer/col 1})))))))

(defn get-namespace
  [buf]
  (let [content (buffer/get-line buf 1)]
    (re-find #"(?<=\(ns )[-a-z0-9\\.]+" content))) ;)


(defn raw-eval-sexp-at-point
  [buf]
  (when (not= (@editor/state ::repeat-counter) 0) (swap! editor/state assoc ::repeat-counter 0))
  (let [sexp (if (= (buf ::buffer/mode) :visuel) (buffer/get-selected-text buf) (buffer/sexp-at-point buf))
        namespace (or (get-namespace buf) "user")]
    (try
      (load-string
        (str "(do (ns " namespace ") (in-ns '" namespace ") " sexp ")"))
      (catch Exception e (println (util/pretty-exception e))))))

(defn sanitice-output
  [x]
  (cond (string? x) x
        (and (map? x) (x ::editor/buffers)) "<editor/state>"
        true (pr-str x)))

(defn eval-sexp-at-point
  [buf]
  (when (not= (@editor/state ::repeat-counter) 0) (swap! editor/state assoc ::repeat-counter 0))
  (let [sexp (if (= (buf ::buffer/mode) :visuel) (buffer/get-selected-text buf) (buffer/sexp-at-point buf))
        namespace (or (get-namespace buf) "user")]
    (create-ns (symbol namespace))
    (binding [*print-length* 200
              *ns* (find-ns (symbol namespace))]
      (editor/message "" :view true); ( :view true :timer 1500)
      (future
        (with-redefs [println (fn [& args] (editor/message (str/join " " args) :append true))]
          (try
            (println (sanitice-output
              (load-string
                (str
                  "(do (ns " namespace ") (in-ns '"
                  namespace
                  ") " sexp "\n)"))))
            (catch Exception e (println (util/pretty-exception e)))))))))

(defn raw-eval-sexp-at-point
  [buf]
  (when (not= (@editor/state ::repeat-counter) 0) (swap! editor/state assoc ::repeat-counter 0))
  (let [sexp (if (= (buf ::buffer/mode) :visuel) (buffer/get-selected-text buf) (buffer/sexp-at-point buf))
        namespace (or (get-namespace buf) "user")]
    (try
      (load-string
        (str "(do (ns " namespace ") (in-ns '" namespace ") " sexp ")"))
      (catch Exception e (println (util/pretty-exception e))))))


(defn evaluate-file-raw
  "Evaluate a given file raw, without using
  with-out-str or other injected functionality.
  If no filepath is supplied the path connected
  to the current buffer will be used."
  ([filepath]
    (try (editor/message (load-file filepath))
      (catch Exception e (editor/message (util/pretty-exception e)))))
  ([] (when-let [filepath ((editor/current-buffer) ::buffer/filename)] (evaluate-file-raw filepath))))

(defn run-command
  [s]
  (let [fargs (str/split s #" ")
        keyw (keyword (first fargs))
        args (rest fargs)]
    (when-let [fun (-> @editor/state ::editor/commands keyw)]
      (apply fun args)))) 


(def commands
  {:left (fn [& args] (repeat-fun buffer/left args))
   :down (fn [& args] (repeat-fun buffer/down args))
   :up (fn [& args] (repeat-fun buffer/up args))
   :right (fn [& args] (repeat-fun buffer/right args))
   :first-non-blank #(non-repeat-fun buffer/first-non-blank)
   :0 #(if (= (@editor/state ::repeat-counter) 0)
         (non-repeat-fun buffer/beginning-of-line)
         (swap! editor/state update ::repeat-counter (fn [t] (* 10 t))))
   :1 #(swap! editor/state update ::repeat-counter (fn [t] (+ (* 10 t) 1)))
   :2 #(swap! editor/state update ::repeat-counter (fn [t] (+ (* 10 t) 2)))
   :3 #(swap! editor/state update ::repeat-counter (fn [t] (+ (* 10 t) 3)))
   :4 #(swap! editor/state update ::repeat-counter (fn [t] (+ (* 10 t) 4)))
   :5 #(swap! editor/state update ::repeat-counter (fn [t] (+ (* 10 t) 5)))
   :6 #(swap! editor/state update ::repeat-counter (fn [t] (+ (* 10 t) 6)))
   :7 #(swap! editor/state update ::repeat-counter (fn [t] (+ (* 10 t) 7)))
   :8 #(swap! editor/state update ::repeat-counter (fn [t] (+ (* 10 t) 8)))
   :9 #(swap! editor/state update ::repeat-counter (fn [t] (+ (* 10 t) 9)))
   :move-matching-paren #(non-repeat-fun buffer/move-matching-paren)
   :word-forward (fn [& args] (repeat-fun buffer/word-forward args))
   :word-forward-ws (fn [& args] (repeat-fun buffer/word-forward-ws args))
   :beginning-of-word (fn [& args] (repeat-fun buffer/beginning-of-word args))
   :end-of-word (fn [& args] (repeat-fun buffer/end-of-word args))
   :end-of-word-ws (fn [& args] (repeat-fun buffer/end-of-word-ws args))
   :end-of-line #(non-repeat-fun buffer/end-of-line)
;   :delete-char (fn [& args] (repeat-fun buffer/delete-char args))
   :delete-char (fn [& args] (repeat-fun #(do (util/set-clipboard-content (str (buffer/get-char %1)) false) (buffer/delete-char %1 %2)) args))
   :copy-selection-to-clipboard #(apply-to-buffer copy-selection-to-clipboard)
   :copy-line copy-line
   :yank-filename yank-filename
   :yank-inner-word (fn [] (non-repeat-fun #(->> % buffer/word-region (yank-region %))))
   :yank-inner-paren (fn [] (non-repeat-fun #(->> % buffer/paren-region (shrink-region %) (yank-region %))))
   :yank-inner-bracket (fn [] (non-repeat-fun #(->> % buffer/bracket-region (shrink-region %) (yank-region %))))
   :yank-inner-brace (fn [] (non-repeat-fun #(->> % buffer/brace-region (shrink-region %) (yank-region %))))
   :yank-inner-quote (fn [] (non-repeat-fun #(->> % buffer/quote-region (shrink-region %) (yank-region %))))
   :yank-outer-paren (fn [] (non-repeat-fun #(->> % buffer/paren-region (yank-region %))))
   :yank-outer-bracket (fn [] (non-repeat-fun #(->> % buffer/bracket-region (yank-region %))))
   :yank-outer-brace (fn [] (non-repeat-fun #(->> % buffer/brace-region (yank-region %))))
   :yank-outer-quote (fn [] (non-repeat-fun #(->> % buffer/quote-region (yank-region %))))

   :delete #(apply-to-buffer delete)
   :delete-line #(non-repeat-fun delete-line)
   :delete-inner-word (fn [] (non-repeat-fun #(->> % buffer/word-region (cut-region %))))
   :delete-inner-paren (fn [] (non-repeat-fun #(->> % buffer/paren-region (shrink-region %) (cut-region %))))
   :delete-inner-bracket (fn [] (non-repeat-fun #(->> % buffer/bracket-region (shrink-region %) (cut-region %))))
   :delete-inner-brace (fn [] (non-repeat-fun #(->> % buffer/brace-region (shrink-region %) (cut-region %))))
   :delete-inner-quote (fn [] (non-repeat-fun #(->> % buffer/quote-region (shrink-region %) (cut-region %))))
   :delete-outer-paren (fn [] (non-repeat-fun #(->> % buffer/paren-region  (cut-region %))))
   :delete-outer-bracket  (fn [] (non-repeat-fun #(->> % buffer/bracket-region  (cut-region %))))
   :delete-outer-brace (fn [] (non-repeat-fun #(->> % buffer/brace-region  (cut-region %))))
   :delete-outer-quote (fn [] (non-repeat-fun #(->> % buffer/quote-region  (cut-region %))))
   :delete-to-word (fn [] (non-repeat-fun #(cut-region % [(% ::buffer/cursor) ((buffer/left (buffer/word-forward %)) ::buffer/cursor)])))
   :delete-to-end-of-word (fn [] (non-repeat-fun #(cut-region % [(% ::buffer/cursor) ((buffer/end-of-word %) ::buffer/cursor)])))

   :change-inner-word (fn [] (non-repeat-fun #(->> % buffer/word-region (cut-region %) set-insert-mode)))
   :change-inner-paren (fn [] (non-repeat-fun #(->> % buffer/paren-region (shrink-region %) (cut-region %) set-insert-mode)))
   :change-inner-bracket (fn [] (non-repeat-fun #(->> % buffer/bracket-region (shrink-region %) (cut-region %) set-insert-mode)))
   :change-inner-brace (fn [] (non-repeat-fun #(->> % buffer/brace-region (shrink-region %) (cut-region %) set-insert-mode)))
   :change-inner-quote (fn [] (non-repeat-fun #(->> % buffer/quote-region (shrink-region %) (cut-region %) set-insert-mode)))
   :change-outer-paren (fn [] (non-repeat-fun #(->> % buffer/paren-region (cut-region %) set-insert-mode)))
   :change-outer-bracket (fn [] (non-repeat-fun #(->> % buffer/bracket-region (cut-region %) set-insert-mode)))
   :change-outer-brace (fn [] (non-repeat-fun #(->> % buffer/brace-region (cut-region %) set-insert-mode)))
   :change-outer-quote (fn [] (non-repeat-fun #(->> % buffer/quote-region (cut-region %) set-insert-mode)))
   :change-line (fn [] (non-repeat-fun #(->> % buffer/line-region (cut-region %) set-insert-mode)))
   :change-eol (fn [] (non-repeat-fun #(->> % buffer/eol-region (cut-region %) set-insert-mode)))
   :change-to-end-of-word (fn [] (non-repeat-fun
                                   #(set-insert-mode
                                      (cut-region % [(% ::buffer/cursor) ((buffer/end-of-word %) ::buffer/cursor)]))))

   :hide-region #(apply-to-buffer buffer/hide-region)
   :unhide-region #(apply-to-buffer buffer/unhide-region)

   :insert-at-line-end #(non-repeat-fun buffer/insert-at-line-end)
   :insert-at-beginning-of-line #(non-repeat-fun buffer/insert-at-beginning-of-line)
   :delete-to-line-end #(non-repeat-fun buffer/delete-to-line-end)

   :append-line #(non-repeat-fun buffer/append-line)
   :append-line-above #(non-repeat-fun (fn [buf] (-> buf buffer/beginning-of-line (buffer/insert-char \newline) buffer/up)))
   :join-lines #(non-repeat-fun buffer/join-lines)
   :join-lines-space #(non-repeat-fun buffer/join-lines-space)

   :eval-sexp-at-point #(eval-sexp-at-point (editor/current-buffer))
   :raw-eval-sexp-at-point #(raw-eval-sexp-at-point (editor/current-buffer))
   :evaluate-file-raw evaluate-file-raw

   :paste-clipboard paste-clipboard
   :paste-clipboard-here paste-clipboard-here
   :beginning-of-buffer #(non-repeat-fun buffer/beginning-of-buffer)
   :navigate-definitions #(typeahead-defs (editor/current-buffer))
   :open-file-at-point open-file-at-point
   :end-of-buffer #(non-repeat-fun buffer/end-of-buffer)
   :scroll-cursor-top (fn [] (non-repeat-fun #(assoc % ::buffer/tow {::buffer/row (-> % ::buffer/cursor ::buffer/row) ::buffer/col 1})))

   :set-visual-mode #(non-repeat-fun buffer/set-visual-mode)
   :set-normal-mode #(non-repeat-fun buffer/set-normal-mode)
   :set-insert-mode #(non-repeat-fun buffer/set-insert-mode)
   :insert-after-point (fn [] (non-repeat-fun #(-> % buffer/set-insert-mode buffer/right)))
   :search  #(non-repeat-fun buffer/search)
   :undo #(non-repeat-fun buffer/undo)
   :q #(editor/exit-program)
   :q! #(editor/force-exit-program)
   :bnext #(editor/oldest-buffer)
   :bn #(editor/oldest-buffer)
   :new #(editor/new-buffer "" {})
   :buffers #(((editor/get-mode :buffer-chooser-mode) :init))
   :ls #(((editor/get-mode :buffer-chooser-mode) :init))
   :previous-regular-buffer editor/previous-regular-buffer
   :w #(write-file)
   :wq #(do (write-file) (editor/exit-program))
   :t #(editor/open-file "/home/sosdamgx/proj/liquid/src/dk/salza/liq/slider.clj")
   :bd #(editor/kill-buffer)
   :bd! #(editor/force-kill-buffer)
   :t1 #(editor/highlight-buffer)
   :ts #(editor/message (buffer/sexp-at-point (editor/current-buffer)))
   :t2 #(editor/message (buffer/get-word (editor/current-buffer)))
   :t3 #(((editor/get-mode :typeahead-mode) :init)
             ["aaa" "bbb" "aabb" "ccc"]
             str
             (fn [res]
               (editor/previous-buffer)
               (editor/apply-to-buffer (fn [buf] (buffer/insert-string buf res)))))
   :t4 #(editor/message (pr-str (buffer/get-line (editor/current-buffer) 1)))
   :t5 #(editor/message (pr-str (:liq2.buffer/lines (editor/current-buffer))))
   :t6 #(((editor/get-mode :info-dialog-mode) :init) "This is the info dialog")
   :! (fn [& args] (external-command (str/join " " args)))
   :e e-cmd
   :edit e-cmd})
 
