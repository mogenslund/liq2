(ns liq2.datastructures.buffer
  (:require [liq2.datastructures.sub-editor :as sub-editor]
            [clojure.string :as str]))

(defn buffer
  [text]
  {::sub-editor (sub-editor/sub-editor text)
   ::encoding :?
   ::major-mode :fundamental-mode
   ::minor-modes []})

