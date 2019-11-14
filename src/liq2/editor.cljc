(ns liq2.editor
  (:require [clojure.string :as str]
            #?(:cljs [lumo.io :as io :refer [slurp spit]])
            [liq2.util :as util]
            [liq2.buffer :as buffer]))

(def state (atom {::buffers {}
                  ::modes {}
                  ::exit-handler nil
                  ::output-handler nil}))

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

(defn get-buffer-id-by-idx
  [idx]
  ((first (filter #(= (% ::idx) idx) (vals (@state ::buffers)))) ::id))

(defn get-buffer-id-by-name
  [name]
  (when-let [buf (first (filter #(= (buffer/get-name %) name) (vals (@state ::buffers))))]
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
  (filter #(not= (subs (str (buffer/get-name %) " ") 0 1) "*") (vals (@state ::buffers))))

(defn get-current-buffer-id
  "Highest idx is current buffer.
  So idx is updated each time buffer is switched."
  []
  (let [idxs (filter number? (map ::idx (vals (@state ::buffers))))]
    (when (not (empty? idxs))
      (get-buffer-id-by-idx (apply max idxs)))))

(comment
  (map ::idx (vals (@state ::buffers)))
  (get-current-buffer-id))

(defn get-current-buffer
  []
  (get-buffer (get-current-buffer-id)))

(defn paint-buffer
  ([buf]
   (when (@state ::output-handler)
     ((@state ::output-handler) buf)))
  ([]
   (paint-buffer (get-current-buffer))))

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
     ;(javax.swing.JOptionPane/showMessageDialog nil (str "IDX " idx " " (pr-str (map ::idx (regular-buffers)))))
     (when idx
       (switch-to-buffer (get-buffer-id-by-idx idx)))))
  ([] (previous-buffer 1)))

(defn oldest-buffer
  []
  (let [idx (first (sort (map ::idx (regular-buffers))))]
    (when idx
      (switch-to-buffer (get-buffer-id-by-idx idx)))))

(defn new-buffer
  [text {:keys [name] :as options}]
  (let [id (util/counter-next)
        o (if (options :rows)
            options
            (let [b (get-current-buffer)]
              (assoc options :top (buffer/get-top b)
                             :left (buffer/get-left b)
                             :rows (buffer/get-rows b)
                             :cols (buffer/get-cols b)))) 
        buf (assoc (buffer/buffer text o) ::id id ::idx id)]
    (swap! state update ::buffers assoc id buf) 
    (switch-to-buffer id)
    (paint-buffer)))

(defn apply-to-buffer
  ([idname fun]
   (if (number? idname)
     (swap! state update-in [::buffers idname] fun)
     (apply-to-buffer (get-buffer-id-by-name idname) fun)))
  ([fun] (apply-to-buffer (get-current-buffer-id) fun)))

(comment
  (new-buffer)
  )

(def tmp-keymap (atom nil))

(defn exit-program
  []
  ((@state ::exit-handler)))

(defn handle-input
  [c]
  ;(spit "/tmp/liq2.log" (str "INPUT: " c "\n"))
  (let [mode (buffer/get-mode (get-current-buffer))
        major-mode (buffer/get-major-mode (get-current-buffer))
        tmp-k (and @tmp-keymap (@tmp-keymap c))
        _ (reset! tmp-keymap nil)
        action (or
                 tmp-k
                 (((get-mode major-mode) mode) c)
                 (when (not= mode :insert) (((get-mode major-mode) :normal) c)))]
    (cond (fn? action) (action)
          (map? action) (reset! tmp-keymap action)
          ;action (swap! state update-in [::buffers (get-current-buffer-id)] (action :function))
          (= mode :insert) (swap! state update-in [::buffers (get-current-buffer-id)] #(buffer/insert-char % (first c)))))
  (paint-buffer))


