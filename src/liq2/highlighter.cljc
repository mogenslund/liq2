(ns liq2.highlighter
  (:require [clojure.string :as str]
            [liq2.buffer :as buffer]))

(defn first-match  
  [s match]
  (if (string? match)
    (when-let [m (str/index-of s match)]
      {:pos (inc m) :match match})
    (let [m (re-find match s)]
      (cond (= m "") {:pos (inc (count s)) :match ""}
            (string? m) (first-match s m) 
            (vector? m) (first-match s (first m))))))

(defn highlight
  [buf hl]
  (loop [b buf row 1 col 1 context :plain]
    (let [line (buffer/get-line b row col)
          hit (first (sort-by :pos
                       (filter :pos
                         (for [[reg con] (-> hl context :matchers)]
                           (assoc (first-match line reg) :context con)))))
          col1 (if (and hit (hit :pos)) (+ (hit :pos) col -1) (buffer/col-count b row))
          next-context (if (and hit (hit :pos)) (hit :context) context)
          match (or (and hit (hit :match)) "")]
      (cond (> row (buffer/line-count b)) b 
            (>= col (buffer/col-count b row)) (recur b (inc row) 1 context)
            true (recur (-> b (buffer/set-style row col (dec col1) (-> hl context :style))
                              (buffer/set-style row col1 (+ col1 (count match)) (-> hl next-context :style)))
                        row
                        (+ col1 (count match) 1)
                        next-context)))))

      
(comment
  (def buf1 (buffer/buffer "Some :keyw and \"text\" and a :keyword ; My \"text\" ; comment and\nSome more text\nHere is also a comment"))

  (nil :pos)

  (first (sort-by :pos
                       (filter :pos
                         (for [[reg con] (-> hl :plain1 :matchers)]
                           (assoc (first-match "lj" reg) :context con)))))
  (sort-by :pos (filter :pos
                        (for [[reg con] (-> hl :plain :matchers)]
                          (assoc (first-match "Some :keyw and" reg) :context con))))
  (sort-by :pos (filter :pos
                        (for [[reg con] (-> hl :plain :matchers)]
                          (assoc (first-match "Some keyw and" reg) :context con))))

  (def match-keys
    {:match-keyword-begin #"(?<=(\s|\(|\[|\{)):[\w\#\.\-\_\:\+\=\>\<\/\!\?\*]+(?=(\s|\)|\]|\}|\,))"
     :match-keyword-end #"."
     :match-string-begin #"(?<!\\\\)(\")"
     :match-string-escape #"(\\\")"
     :match-string-end #"(\")"
     :match-comment-begin #"(?<!\\\\);"
     :match-comment-end #"$"})

  (def hl1
    {:plain ; Context
      {:style :plain1 ; style
       :matchers {(match-keys :match-string-begin) :string
                  (match-keys :match-keyword-begin) :keyword
                  (match-keys :match-comment-begin) :comment}}
     :string
      {:style :string
       :matchers {(match-keys :match-string-escape) :string
                  (match-keys :match-string-end) :plain}}
     :comment
      {:style :comment
       :matchers {(match-keys :match-comment-end) :plain}}
     :keyword
      {:style :keyword
       :matchers {(match-keys :match-keyword-end) :plain}}})

  (first-match #"$" "abc")
  (pr-str (re-find #"$" "abc"))

  (let [buf (highlight (buffer/buffer "a :bbb ccc") hl1)]
    (map #(buffer/get-style buf 1 %) (range 1 (buffer/col-count buf 1))))
  (highlight buf1 hl1))
