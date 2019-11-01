(ns liq2.util)

(def counter (atom 0))
(defn counter-next
  []
  (swap! counter inc))
