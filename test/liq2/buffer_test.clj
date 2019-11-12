(ns liq2.buffer-test
  (:require [clojure.test :refer :all]
            [liq2.buffer :refer :all]))

(deftest forward-char-test
  (testing "Forward char"
    (let [buf (buffer "abc\n\ndef")]
      (is (= (get-point buf) (point 1 1)))
      (is (= (-> buf
                 forward-char
                 get-point)
             (point 1 2)))
      (is (= (-> buf
                 (forward-char 10)
                 get-point)
             (point 1 3)))
      (is (= (-> buf
                 next-line
                 forward-char
                 get-point)
             (point 2 1))))))

(deftest get-text-test
  (testing "Get text"
    (let [buf (buffer "abc\n\ndef")]
      (is (= (get-text buf) "abc\n\ndef"))
      (is (= (get-text buf (point 1 1) (point 1 10)) "abc"))
      (is (= (get-text buf (point 1 1) (point 1 3)) "abc"))
      (is (= (get-text buf (point 1 4) (point 2 1)) "\n"))
      (is (= (get-text buf (point 1 4) (point 9 1)) "\n\ndef"))
      (is (= (get-text buf (point 4 10) (point 4 20)) ""))
      (is (= (get-text buf (point 5 1) (point 5 2)) ""))
      (is (= (get-text buf (point 1 10) (point 1 1)) "abc")))))
