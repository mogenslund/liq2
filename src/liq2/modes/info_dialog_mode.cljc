(ns liq2.modes.info-dialog-mode
  (:require [clojure.string :as str]
            [liq2.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq2.buffer :as buffer]
            [liq2.util :as util]))

(defn content
  [text]
  (str " ==============================================\n"
       "                                              \n"
       "      " text "\n"
       "                                              \n"
       " ==============================================\n"))

(defn run
  [text]
  (let [id (editor/get-buffer-id-by-name "*info-dialog*")]
    (if id
      (switch-to-buffer id)
      (editor/new-buffer "" {:major-mode :info-dialog-mode :name "*info-dialog*" :top 10 :left 10 :rows 7 :cols 60}))
    (apply-to-buffer #(-> % buffer/clear
                            (buffer/insert-string (content text))
                            buffer/beginning-of-buffer))))
    
(def mode
  {:insert {"\n" editor/previous-buffer}
   :normal {"\n" editor/previous-buffer}
   :visual {"\n" editor/previous-buffer}
   :init run})