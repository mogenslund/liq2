(ns liq2.modes.typeahead-mode
  (:require [clojure.string :as str]
            [liq2.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq2.buffer :as buffer]
            [liq2.util :as util]))

(def ^:private state
  (atom {::tostringfun nil
         ::callback nil
         ::items nil
         ::filtered nil
         ::oldsearch ""
         ::search ""}))

(defn- update-state
  []
  (let [st @state
        pat (re-pattern (str "(?i)" (str/replace (st ::search) #" " ".*")))
        filtered (take 150 (filter #(re-find pat ((st ::tostringfun)  %)) (st ::items)))]
    (swap! state assoc ::filtered filtered)))

(defn update-view
  [buf]
  (let [st (update-state)]
    (-> buf
        buffer/clear
        (buffer/insert-string
          (str 
             (st ::search)
             "\n"
             (str/join "\n" (map (st ::tostringfun) (st ::filtered)))))
        buffer/beginning-of-buffer
        buffer/insert-at-line-end)))

(defn run
  [items tostringfun callback]
  (if-let [id (editor/get-buffer-id-by-name "*typeahead*")]
    (switch-to-buffer id)
    (editor/new-buffer "" {:major-mode :typeahead-mode :name "*typeahead*"}))
  (swap! state assoc
    ::tostringfun tostringfun
    ::callback callback
    ::items items
    ::search ""
    ::filtered items)
  (apply-to-buffer update-view))

(defn execute
  []
  (let [st @state
        index (- (-> (editor/current-buffer) ::buffer/cursor ::buffer/row) 2)
        res (first (drop index (st ::filtered)))] 
    ((st ::callback) res)))

(defn handle-input
  [c]
  (if-let [f ({"esc" (fn [] (apply-to-buffer #(-> % buffer/set-normal-mode buffer/left)))
               "C-n" (fn [] (apply-to-buffer #(-> % buffer/set-normal-mode buffer/left buffer/down)))
               "down" (fn [] (apply-to-buffer #(-> % buffer/set-normal-mode buffer/left buffer/down)))
               "backspace" (fn [] (swap! state update ::search #(subs % 0 (max (dec (count %)) 0)))
                                  (apply-to-buffer update-view))}
              c)]
    (f)
    (do
      (swap! state update ::search #(str % c))
      (apply-to-buffer update-view))))

(def mode
  {:insert handle-input
   :normal {"q" editor/previous-buffer
            "esc" editor/previous-buffer
            "\n" execute
            "i" #(apply-to-buffer buffer/set-insert-mode)
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
            ":" (fn [] (switch-to-buffer "*minibuffer*")
                       (apply-to-buffer #(-> % buffer/clear (buffer/insert-char \:))))}
    :visual {"esc" #(apply-to-buffer buffer/set-normal-mode)}
    :init run})
