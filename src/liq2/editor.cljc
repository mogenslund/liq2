(ns liq2.editor
  (:require [clojure.string :as str]
            #?(:cljs [lumo.io :as io :refer [slurp spit]])
            [liq2.util :as util]
            [liq2.highlighter :as highlighter]
            [liq2.buffer :as buffer]))

(def state (atom {::commands {}
                  ::buffers {}
                  ::modes {}
                  ::settings {:auto-switch-to-output true}
                  ::exit-handler nil
                  ::window nil
                  ::output-handler nil}))

(def ^:private macro-seq (atom ())) ; Macrofunctionality might belong to input handler.
(def ^:private macro-record (atom false))

(defn get-window
  []
  (if-let [w (@state ::window)]
    w
    (let [d ((-> @state ::output-handler :dimensions))
          w {::buffer/top 1 ::buffer/left 1 ::buffer/rows (d :rows) ::buffer/cols (d :cols)}]
      (swap! state assoc ::window w)
      w)))

(defn set-setting
  [keyw value]
  (swap! state assoc-in [::settings keyw] value))

(defn get-setting
  [keyw]
  ((@state ::settings) keyw))

(defn set-output-handler
  [output-handler]
  (swap! state assoc ::output-handler output-handler))

(defn set-exit-handler
  [exit-handler]
  (swap! state assoc ::exit-handler exit-handler))

(defn add-mode
  [keyw mode]
  (swap! state update ::modes assoc keyw mode))

(defn get-mode
  [keyw]
  (or ((@state ::modes) keyw) {}))

(defn add-key-bindings
  [major-mode mode keybindings]
  (swap! state update-in [::modes major-mode mode] #(merge-with merge % keybindings))) 

(defn get-buffer-id-by-idx
  [idx]
  ((first (filter #(= (% ::idx) idx) (vals (@state ::buffers)))) ::id))

(defn get-buffer-id-by-name
  [name]
  (when-let [buf (first (filter #(= (% ::buffer/name) name) (vals (@state ::buffers))))]
    (buf ::id)))

(defn get-buffer
  [idname]
  (if (number? idname)
    ((@state ::buffers) idname)
    (get-buffer (get-buffer-id-by-name idname))))

(defn all-buffers
  []
  (reverse (sort-by ::idx (vals (@state ::buffers)))))

(defn regular-buffers
  []
  (filter #(not= (subs (str (% ::buffer/name) " ") 0 1) "*") (vals (@state ::buffers))))

(defn current-buffer-id
  "Highest idx is current buffer.
  So idx is updated each time buffer is switched."
  []
  (let [idxs (filter number? (map ::idx (vals (@state ::buffers))))]
    (when (not (empty? idxs))
      (get-buffer-id-by-idx (apply max idxs)))))

(comment
  (map ::idx (vals (@state ::buffers)))
  (current-buffer-id))

(defn current-buffer
  []
  (get-buffer (current-buffer-id)))

(defn switch-to-buffer
  [idname]
  (if (number? idname)
    (do
      (swap! state assoc-in [::buffers idname ::idx] (util/counter-next))
      idname)
    (switch-to-buffer (get-buffer-id-by-name idname)))) 

(defn previous-buffer
  "n = 1 means previous"
  ([n]
   (let [idx (first (drop n (reverse (sort (map ::idx (vals (@state ::buffers)))))))]
     (when idx
       (switch-to-buffer (get-buffer-id-by-idx idx)))))
  ([] (previous-buffer 1)))

(defn previous-regular-buffer-id
  "n = 1 means previous"
  ([n]
   (let [idx (first (drop n (reverse (sort (map ::idx (vals (@state ::buffers)))))))]
     (when idx
       (get-buffer-id-by-idx idx))))
  ([] (previous-regular-buffer-id 1)))

(defn previous-regular-buffer
  "n = 1 means previous"
  ([n]
   (let [idx (first (drop n (reverse (sort (map ::idx (regular-buffers))))))]
     (when idx
       (switch-to-buffer (get-buffer-id-by-idx idx)))))
  ([] (previous-regular-buffer 1)))

(defn oldest-buffer
  []
  (let [idx (first (sort (map ::idx (regular-buffers))))]
    (when idx
      (switch-to-buffer (get-buffer-id-by-idx idx)))))

(defn apply-to-buffer
  ([idname fun]
   (if (number? idname)
     (swap! state update-in [::buffers idname] fun)
     (apply-to-buffer (get-buffer-id-by-name idname) fun)))
  ([fun] (apply-to-buffer (current-buffer-id) fun)))

(comment
  (new-buffer)
  )

(defn highlight-paren
  [buf]
  (if ({\( \) \) \( \{ \} \} \{ \[ \] \] \[} (buffer/get-char buf))
    (let [r (buffer/paren-matching-region buf (buf ::buffer/cursor))
          p (second r)]
      (if p
        (buffer/set-style buf p :red) 
        buf))
    buf))

(defn paint-buffer-old
  ([buf]
   (when (@state ::output-handler)
     (apply-to-buffer "*status-line*"
       #(-> %
            buffer/clear
            (buffer/insert-string
              (str (or (buf ::buffer/filename) (buf ::buffer/name)) "  "
                   (if (buffer/dirty? buf) " [+] " "     ")
                   (cond (= (buf ::buffer/mode) :insert) "-- INSERT --   "
                         (= (buf ::buffer/mode) :visual) "-- VISUAL --   "
                         true "               ")
                   (-> buf ::buffer/cursor ::buffer/row) "," (-> buf ::buffer/cursor ::buffer/col)))
            buffer/beginning-of-buffer))
     ;((@state ::output-handler) (get-buffer "*status-line*"))
       ((-> @state ::output-handler :printer) (assoc (highlight-paren buf) :status-line (get-buffer "*status-line*")))))
  ([] (paint-buffer-old (current-buffer))))

(defn paint-buffer
  ([nameid]
   (when (@state ::output-handler)
     (apply-to-buffer nameid buffer/update-tow)
     (let [buf (get-buffer nameid)]
       (apply-to-buffer "*status-line*"
         #(-> %
              buffer/clear
              (buffer/insert-string
                (str (or (buf ::buffer/filename) (buf ::buffer/name)) "  "
                     (if (buffer/dirty? buf) " [+] " "     ")
                     (cond (= (buf ::buffer/mode) :insert) "-- INSERT --   "
                           (= (buf ::buffer/mode) :visual) "-- VISUAL --   "
                           true "               ")
                     (-> buf ::buffer/cursor ::buffer/row) "," (-> buf ::buffer/cursor ::buffer/col)))
              buffer/beginning-of-buffer
              buffer/update-tow))
       ;((@state ::output-handler) (get-buffer "*status-line*"))
         ((-> @state ::output-handler :printer) (assoc (highlight-paren buf) :status-line (get-buffer "*status-line*"))))))
  ([] (paint-buffer (current-buffer-id))))

(defn message
  [s & {:keys [:append :view :timer]}]
  (if append
    ;(apply-to-buffer "output" #(-> % (buffer/append-buffer (buffer/buffer (str s "\n"))) buffer/end-of-buffer))
    (apply-to-buffer "output" #(-> % buffer/end-of-buffer (buffer/insert-string (str s "\n")) buffer/end-of-buffer))
    (apply-to-buffer "output" #(-> % buffer/clear (buffer/insert-string (str s)))))
  (paint-buffer "output")
  (when (and view (get-setting :auto-switch-to-output))
    (switch-to-buffer "output")
    (when timer (future (Thread/sleep timer) (previous-buffer) (paint-buffer))))
  (paint-buffer))

(defn force-kill-buffer
  ([idname]
   (when (> (count (regular-buffers)) 1)
     (let [id (if (number? idname) idname (get-buffer-id-by-name idname))]
       (swap! state update ::buffers dissoc id))
       (previous-regular-buffer)))
  ([] (force-kill-buffer (current-buffer-id))))

(defn kill-buffer
  ([idname]
   (if (not (buffer/dirty? (get-buffer idname)))
     (force-kill-buffer idname)
     (message "There are unsaved changes. Use bd! to force kill." :view true :timer 1500)))
  ([] (kill-buffer (current-buffer-id))))

(defn highlight-buffer
  ([idname]
   (apply-to-buffer idname
     (fn [buf]
       (let [hl ((get-mode (buf ::buffer/major-mode)) :syntax)]
         (if hl
           (highlighter/highlight buf hl)
           buf)))))
  ([] (highlight-buffer (current-buffer-id))))

(defn highlight-buffer-row
  ([idname]
   (apply-to-buffer idname
     (fn [buf]
       (let [hl ((get-mode (buf ::buffer/major-mode)) :syntax)]
         (if hl
           (highlighter/highlight buf hl (-> buf ::buffer/cursor ::buffer/row))
           buf)))))
  ([] (highlight-buffer-row (current-buffer-id))))

(defn new-buffer
  [text {:keys [name] :as options}]
  (let [id (util/counter-next)
        o (if (options :rows)
            options
            (let [b (current-buffer) ;; TODO If there is no current-buffer, there will be a problem!
                  w (b ::buffer/window)] 
              (assoc options :top (w ::buffer/top)
                             :left (w ::buffer/left)
                             :rows (w ::buffer/rows)
                             :cols (w ::buffer/cols)))) 
        buf (assoc (buffer/buffer text o) ::id id ::idx id)]
    (swap! state update ::buffers assoc id buf) 
    (highlight-buffer id)
    (switch-to-buffer id)
    (paint-buffer)))

(defn open-file
  [path]
  (let [p (util/resolve-home path)]
    (if (get-buffer-id-by-name p)
      (switch-to-buffer p)
      (new-buffer (or (util/read-file p) "") {:name p :filename p}))))

(defn dirty-buffers
  []
  (filter buffer/dirty? (all-buffers)))

(defn force-exit-program
  []
  ((@state ::exit-handler)))

(defn exit-program
  []
  (let [bufs (dirty-buffers)]
    (if (empty? bufs)
      (force-exit-program)
      (do
        (message (str
          "There are unsaved files. Use :q! to force quit:\n"
          (str/join "\n" (map ::buffer/filename bufs))) :view true :timer 1500)))))

(def tmp-keymap (atom nil))

(defn handle-input
  [c]
  ;(spit "/tmp/liq2.log" (str "INPUT: " c "\n"))
  (when (and @macro-record (not= c "Q"))
    (swap! macro-seq conj c))
  (let [mode ((current-buffer) ::buffer/mode)
        major-mode ((current-buffer) ::buffer/major-mode)
        tmp-k-selfinsert (and @tmp-keymap (@tmp-keymap :selfinsert)) 
        tmp-k (and @tmp-keymap
                   (or (@tmp-keymap c)
                       (and tmp-k-selfinsert
                            (fn [] (apply-to-buffer #(tmp-k-selfinsert % c))))))
        _ (reset! tmp-keymap nil)
        action (or
                 tmp-k
                 (((get-mode major-mode) mode) c)
                 (when (not= mode :insert) (((get-mode major-mode) :normal) c)))]
    (cond (fn? action) (action)
          (keyword? action) (when (-> @state ::commands action) ((-> @state ::commands action)))
          (map? action) (reset! tmp-keymap action)
          ;action (swap! state update-in [::buffers (current-buffer-id)] (action :function))
          (= mode :insert) (apply-to-buffer #(buffer/insert-char % (first c))))
    (cond (= c "esc") (highlight-buffer) ; TODO Maybe not highlight from scratch each time
          (= mode :insert) (highlight-buffer-row))
    (paint-buffer)))

(defn record-macro
  []
  (if (not @macro-record)
    (do
      (message "Recording macro")
      (reset! macro-seq ()))
    (message "Recording finished"))
  (swap! macro-record not))

(defn run-macro
  []
  (when (not @macro-record)
    (doall (map handle-input (reverse @macro-seq)))))