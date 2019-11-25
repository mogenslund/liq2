(ns liq2.modes.fundamental-mode-test
  (:require [clojure.test :refer :all]
            [liq2.buffer :as buffer]
            [liq2.modes.fundamental-mode :refer :all]))

(deftest get-namespace-test
  (testing "Get namespace"
    (is (nil? (get-namespace (buffer/buffer ""))))
    (is (= (get-namespace (buffer/buffer "(ns abc.def)\n  asdf")) "abc.def"))
    (is (= (get-namespace (buffer/buffer "(ns abc.def\n  asdf)")) "abc.def"))))

(deftest eval-sexp-at-point-test
  (testing "Eval sexp at point"
    (let [buf (-> (buffer/buffer "(ns abc.def)\n(+ 1 2 3)")
                  (buffer/set-point 2 3))]
      (is (= (eval-sexp-at-point buf) 6)))))