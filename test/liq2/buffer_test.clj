(ns liq2.buffer-test
  (:require [clojure.test :refer :all]
            [liq2.buffer :refer :all]))

(deftest forward-char-test
  (testing "Forward char"
    (let [buf (buffer "abc\n\ndef")]
      (is (= (get-point buf) (point 1 1))))))
