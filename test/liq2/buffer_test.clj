(ns liq2.buffer-test
  (:require [clojure.test :refer :all]
            [liq2.buffer :refer :all]))

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

(defn random-string
  [len]
  (let [chars ["a" "b" "c" "d" "e" "f" "g" "h" "i" "j" "k" "l" "m" "æ" "A" "B" "-" " " "\n"]]
    (apply str (repeatedly len (fn [] (rand-nth chars))))
  ))

(defn random-textoperation
  [buf]
  (let [r (rand-int 12)]
    (cond (= r 0) (right buf 1)
          (= r 1) (right buf (rand-int 20))
          (= r 2) (left buf 1)
          (= r 3) (left buf (rand-int 20))
          (= r 4) (delete-char buf 1)
          (= r 5) (delete-char buf (rand-int 3))
          ;(= r 6) (end-of-line buf)
          ;(= r 7) (end-of-word buf)
          ;(= r 8) (beginning-of-buffer buf)
          ;(= r 9) (set-meta buf :something "abc")
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