(ns liq2.editor
  (:require [clojure.string :as str]
            #?(:cljs [lumo.io :as io :refer [slurp spit]])
            [liq2.util :as util]
            [liq2.frame :as frame]
            [liq2.buffer :as buffer]))

(def state (atom {::buffers {}
                  ::modes {}
                  ::frames {}
                  ::output-handler nil}))

(defn set-output-handler
  [output-handler]
  (swap! state assoc ::output-handler output-handler))

(defn add-mode
  [keyw mode]
  (swap! state update ::modes assoc keyw mode))

(defn get-mode
  [keyw]
  (or ((@state ::modes) keyw) {}))

(defn get-buffer
  [id]
  ((@state ::buffers) id))

(defn get-buffer-id-by-idx
  [idx]
  ((first (filter #(= (% ::idx) idx) (vals (@state ::buffers)))) ::id))

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

(defn new-frame
  [top left rows cols]
  (let [fr (frame/frame top left rows cols)]
    (swap! state update ::frames assoc (frame/get-id fr) fr)
    (frame/get-id fr)))

(defn get-frame
  [id]
  ((@state ::frames) id))

(defn get-frame-id-from-buffer-id
  "Given index of buffer, return id of frame."
  [id]
  (when-let [fr (first (filter #(= (frame/get-buffer-id %) id) (vals (@state ::frames))))]
    (frame/get-id fr)))

(defn get-buffer-frame
  [id]
  (get-frame (get-frame-id-from-buffer-id id)))

(defn get-empty-frames
  []
  (filter #(nil? (frame/get-buffer-id %)) (vals (@state ::frames))))

(defn switch-to-buffer
  [id]
  (let [empty-frame (first (get-empty-frames))
        frameid (get-frame-id-from-buffer-id (get-current-buffer-id))]
    (when (not (get-frame-id-from-buffer-id id))
      (if empty-frame
        (swap! state update-in [::frames (frame/get-id empty-frame)] #(frame/set-buffer-id % id))  
        (swap! state update-in [::frames frameid] #(frame/set-buffer-id % id)))))
  (swap! state assoc-in [::buffers id ::idx] (util/counter-next))
  id)

(defn previous-buffer
  "n = 1 means previous"
  ([n]
   (let [idx (first (drop n (reverse (sort (map ::idx (vals (@state ::buffers)))))))]
     (when idx
       (switch-to-buffer (get-buffer-id-by-idx idx)))))
  ([] (previous-buffer 1)))

(defn new-buffer
  []
  (let [id (util/counter-next)
        buf (assoc (buffer/buffer "") ::id id)]
    (swap! state update ::buffers assoc id buf) 
    (switch-to-buffer id)))

(defn apply-to-buffer
  ([id fun]
   (swap! state update-in [::buffers id] fun))
  ([fun] (apply-to-buffer (get-current-buffer-id) fun)))

(comment
  (new-buffer)
  (new-frame 10 10 10 10)
  (get-empty-frames)
  )

(defn push-output
  []
  ((@state ::output-handler) (get-current-buffer) (get-buffer-frame (get-current-buffer-id)))) 

(def tmp-keymap (atom nil))


(defn handle-input
  [c]
  ;(spit "/tmp/liq2.log" (str "INPUT: " c "\n"))
  (let [mode (buffer/get-mode (get-current-buffer))
        major-mode (buffer/get-major-mode (get-current-buffer))
        tmp-k (and @tmp-keymap (@tmp-keymap c))
        _ (reset! tmp-keymap nil)
        action (or
                 tmp-k
                 (((get-mode major-mode) mode) c))]
    (cond (fn? action) (action)
          (and action (action :keymap)) (reset! tmp-keymap (action :keymap))
          action (swap! state update-in [::buffers (get-current-buffer-id)] (action :function))
          (= mode :insert) (swap! state update-in [::buffers (get-current-buffer-id)] #(buffer/insert-char % (first c)))))
  (push-output))


