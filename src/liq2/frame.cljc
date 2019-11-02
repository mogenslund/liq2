(ns liq2.frame
  (:require [liq2.util :as util]
            [clojure.string :as str]))

(defn frame
  [top left rows cols]
  {::id (util/counter-next)
   ::top top
   ::left left
   ::rows rows 
   ::cols cols
   ::buffer-id nil})

(defn get-top
  [fr]
  (fr ::top))

(defn get-left
  [fr]
  (fr ::left))

(defn get-rows
  [fr]
  (fr ::rows))

(defn get-cols
  [fr]
  (fr ::cols))

(defn get-id
  [fr]
  (fr ::id))

(defn set-buffer-id
  [fr id]
  (assoc fr ::buffer-id id))

(defn get-buffer-id
  [fr]
  (fr ::buffer-id))

(defn split-vertically
  [fr])