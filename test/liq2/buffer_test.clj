(ns liq2.buffer-test
  (:require [clojure.test :refer :all]
            [liq2.buffer :refer :all]))

(deftest sub-buffer-test
  (testing "Sub buffer"
    (is (= (-> (buffer "aaa\nbbb\nccc\nddd\neee") (sub-buffer 2 3) get-text)
           "bbb\nccc"))
    (is (= (-> (buffer "aaa\nbbb\nccc\nddd\neee") (sub-buffer 1 3) get-text)
           "aaa\nbbb\nccc"))
    (is (= (-> (buffer "aaa\nbbb\nccc\nddd\neee") down down down right (sub-buffer 2 4) :liq2.buffer/cursor :liq2.buffer/row)
           3))
    (is (= (-> (buffer "aaa\nbbb\nccc\nddd\neee") down down down right (sub-buffer 1 4) :liq2.buffer/cursor :liq2.buffer/row)
           4))))

(deftest line-count-test
  (testing "Line count"
    (is (= (-> (buffer "") line-count) 1)) 
    (is (= (-> (buffer "abc") line-count) 1)) 
    (is (= (-> (buffer "abc\n") line-count) 2)) 
    (is (= (-> (buffer "abc  \n  abc") line-count) 2)) 
    (is (= (-> (buffer "abc\n\n") line-count) 3)) 
    (is (= (-> (buffer "abc\n\nabc") line-count) 3)) 
    (is (= (-> (buffer "abc\n\n") end-of-buffer line-count) 3)) 
    (is (= (-> (buffer "abc\n\nabc") end-of-buffer line-count) 3))))

(deftest col-count-test
  (testing "Col count"
    (is (= (-> (buffer "") (col-count 1)) 0)) 
    (is (= (-> (buffer "a") (col-count 1)) 1)) 
    (is (= (-> (buffer "a\n") (col-count 1)) 1)) 
    (is (= (-> (buffer "a\n") (col-count 2)) 0)) 
    (is (= (-> (buffer "a\n") (col-count 10)) 0)) 
    (is (= (-> (buffer "a\nabc") (col-count 2)) 3))))


(deftest right-test
  (testing "Forward char"
    (let [buf (buffer "abc\n\ndef")]
      (is (= (buf :liq2.buffer/cursor) {:liq2.buffer/row 1 :liq2.buffer/col 1}))
      (is (= (-> buf
                 right
                 :liq2.buffer/cursor)
             {:liq2.buffer/row 1 :liq2.buffer/col 2}))
      (is (= (-> buf
                 (right 10)
                 :liq2.buffer/cursor)
             {:liq2.buffer/row 1 :liq2.buffer/col 3}))
      (is (= (-> buf
                 down
                 right
                 :liq2.buffer/cursor)
             {:liq2.buffer/row 2 :liq2.buffer/col 1})))))

(deftest point-compare-test
  (testing "Point compare"
    (is (= (point-compare {:liq2.buffer/row 1 :liq2.buffer/col 1} {:liq2.buffer/row 1 :liq2.buffer/col 2}) -1))
    (is (= (point-compare {:liq2.buffer/row 1 :liq2.buffer/col 10} {:liq2.buffer/row 1 :liq2.buffer/col 2}) 1))
    (is (= (point-compare {:liq2.buffer/row 2 :liq2.buffer/col 1} {:liq2.buffer/row 1 :liq2.buffer/col 2}) 1))
    (is (= (point-compare {:liq2.buffer/row 1 :liq2.buffer/col 2} {:liq2.buffer/row 1 :liq2.buffer/col 2}) 0))))

(deftest get-line-test
  (testing "Get line"
    (is (= (-> (buffer "") get-line) ""))
    (is (= (-> (buffer "aaa") (get-line 1)) "aaa"))
    (is (= (-> (buffer "aaa") (get-line 2)) ""))
    (is (= (-> (buffer "\naaa") (get-line 2)) "aaa"))))

(deftest get-text-test
  (testing "Get text"
    (let [buf (buffer "abc\n\ndef")]
      (is (= (get-text buf) "abc\n\ndef"))
      (is (= (get-text buf {:liq2.buffer/row 1 :liq2.buffer/col 1} {:liq2.buffer/row 1 :liq2.buffer/col 10}) "abc"))
      (is (= (get-text buf {:liq2.buffer/row 1 :liq2.buffer/col 1} {:liq2.buffer/row 1 :liq2.buffer/col 3}) "abc"))
      (is (= (get-text buf {:liq2.buffer/row 1 :liq2.buffer/col 4} {:liq2.buffer/row 2 :liq2.buffer/col 1}) "\n"))
      (is (= (get-text buf {:liq2.buffer/row 1 :liq2.buffer/col 4} {:liq2.buffer/row 9 :liq2.buffer/col 1}) "\n\ndef"))
      (is (= (get-text buf {:liq2.buffer/row 4 :liq2.buffer/col 10} {:liq2.buffer/row 4 :liq2.buffer/col 20}) ""))
      (is (= (get-text buf {:liq2.buffer/row 5 :liq2.buffer/col 1} {:liq2.buffer/row 5 :liq2.buffer/col 2}) ""))
      (is (= (get-text buf {:liq2.buffer/row 1 :liq2.buffer/col 10} {:liq2.buffer/row 1 :liq2.buffer/col 1}) "abc")))))

(deftest delete-region-test
  (testing "Delete region"
    (is (= (-> (buffer "abc") (delete-region [{:liq2.buffer/row 1 :liq2.buffer/col 1} {:liq2.buffer/row 1 :liq2.buffer/col 3}]) get-text) ""))
    (is (= (-> (buffer "abc") (delete-region [{:liq2.buffer/row 1 :liq2.buffer/col 1} {:liq2.buffer/row 1 :liq2.buffer/col 4}]) get-text) ""))
    (is (= (-> (buffer "abc") (delete-region [{:liq2.buffer/row 1 :liq2.buffer/col 4} {:liq2.buffer/row 1 :liq2.buffer/col 4}]) get-text) "abc"))
    (is (= (-> (buffer "abc") (delete-region [{:liq2.buffer/row 1 :liq2.buffer/col 1} {:liq2.buffer/row 1 :liq2.buffer/col 5}]) get-text) ""))
    (is (= (-> (buffer "abc") (delete-region [{:liq2.buffer/row 1 :liq2.buffer/col 1} {:liq2.buffer/row 1 :liq2.buffer/col 1}]) get-text) "bc"))
    (is (= (-> (buffer "") (delete-region [{:liq2.buffer/row 1 :liq2.buffer/col 1} {:liq2.buffer/row 1 :liq2.buffer/col 1}]) get-text) ""))
    (is (= (-> (buffer "a\n") (delete-region [{:liq2.buffer/row 1 :liq2.buffer/col 2} {:liq2.buffer/row 2 :liq2.buffer/col 0}]) get-text) "a"))
    (is (= (-> (buffer "\n") (delete-region [{:liq2.buffer/row 1 :liq2.buffer/col 0} {:liq2.buffer/row 2 :liq2.buffer/col 0}]) get-text) ""))
    (is (= (-> (buffer "a\n") (delete-region [{:liq2.buffer/row 1 :liq2.buffer/col 2} {:liq2.buffer/row 2 :liq2.buffer/col 1}]) get-text) "a"))
    (is (= (-> (buffer "aa\n") (delete-region [{:liq2.buffer/row 1 :liq2.buffer/col 1} {:liq2.buffer/row 2 :liq2.buffer/col 0}]) get-text) ""))
    (is (= (-> (buffer "abc") (delete-region [{:liq2.buffer/row 1 :liq2.buffer/col 1} {:liq2.buffer/row 1 :liq2.buffer/col 3}]) get-text) ""))))

(deftest split-buffer-test
  (testing "Split buffer"
    (is (= (map get-text (split-buffer (buffer ""))) (list "" "")))
    (is (= (map get-text (split-buffer (buffer "aabb") {:liq2.buffer/row 1 :liq2.buffer/col 1})) (list "" "aabb")))
    (is (= (map get-text (split-buffer (buffer "aabb") {:liq2.buffer/row 1 :liq2.buffer/col 2})) (list "a" "abb")))
    (is (= (map get-text (split-buffer (buffer "aabb") {:liq2.buffer/row 1 :liq2.buffer/col 3})) (list "aa" "bb")))
    (is (= (map get-text (split-buffer (buffer "aabb") {:liq2.buffer/row 1 :liq2.buffer/col 4})) (list "aab" "b")))
    (is (= (map get-text (split-buffer (buffer "aabb") {:liq2.buffer/row 1 :liq2.buffer/col 5})) (list "aabb" "")))
    (is (= (map get-text (split-buffer (buffer "aa\nbb") {:liq2.buffer/row 2 :liq2.buffer/col 1})) (list "aa\n" "bb")))
    (is (= (map get-text (split-buffer (buffer "aa\n") {:liq2.buffer/row 2 :liq2.buffer/col 1})) (list "aa\n" "")))
    (is (= (map get-text (split-buffer (buffer "aa\n") {:liq2.buffer/row 2 :liq2.buffer/col 0})) (list "aa\n" "")))
    (is (= (map get-text (split-buffer (buffer "\nbb") {:liq2.buffer/row 1 :liq2.buffer/col 1})) (list "" "\nbb")))
    (is (= (map get-text (split-buffer (buffer "aabb") {:liq2.buffer/row 1 :liq2.buffer/col 3})) (list "aa" "bb")))))

(defn random-string
  [len]
  (let [chars ["a" "b" "c" "d" "e" "f" "g" "h" "i" "j" "k" "l" "m" "æ" "A" "B" "-" " " "\n" "\r" "$" "\t" "-" ":"]]
    (apply str (repeatedly len (fn [] (rand-nth chars))))
  ))

(defn random-buffer
  []
  (let [n (rand-int 20)]
    (cond (= n 0) (buffer "")
          (= n 1) (buffer "\n")
          (= n 2) (buffer "\n\n")
          (= n 3) (buffer "a")
          (= n 4) (buffer "\na")
          (= n 5) (buffer "a\n")
          true (buffer (random-string (rand-int 50000))))))

(defn random-textoperation
  [buf]
  (let [r (rand-int 12)]
    (cond (= r 0) (right buf 1)
          (= r 1) (right buf (rand-int 20))
          (= r 2) (left buf 1)
          (= r 3) (left buf (rand-int 20))
          (= r 4) (delete-char buf 1)
          (= r 5) (delete-char buf (rand-int 3))
          (= r 6) (end-of-line buf)
          (= r 7) (end-of-word buf)
          (= r 8) (beginning-of-buffer buf)
          :else (insert-string buf (random-string (rand-int 100))))))

(defn generate
  [n]
  (nth (iterate random-textoperation (buffer "")) n))

(deftest properties-test
  (doseq [n (range 20)]
    (let [buf (generate (rand-int 500))]
      ;(testing "Point = count before"
      ;  (is (= (get-point buf) (-> buf before get-text count))))
      ;(testing "Linenumber = Total lines in before"
      ;  (is (= (get-linenumber buf) (total-lines (before sl)))))
      ;(testing "Totallines = total lines before and total lines after - 1"
      ;  (is (= (total-lines buf)
      ;         (+ (total-lines (before buf))
      ;            (total-lines (after buf))
      ;            -1))))
      (testing "Insert string -> delete (count string) is invariant"
        (let [len (rand-int 100)
              text (random-string len)]
          (is (= (buf (-> buf (insert-string text) (delete-char len)))))))
    )))

(deftest algebraic-properties-test
  (doseq [n (range 100)]
    (let [b1 (random-buffer)
          b2 (random-buffer)]
      (is (= (get-text (append-buffer b1 b2)) (str (get-text b1) (get-text b2))))
      (is (= (get-text (append-buffer b1 (buffer ""))) (get-text b1)))
      )))