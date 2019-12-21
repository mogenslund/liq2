(ns liq2.commands-test
  (:require [clojure.test :refer :all]
            [liq2.buffer :as buffer]
            [liq2.commands :refer :all]))

(deftest get-namespace-test
  (testing "Get namespace"
    (is (nil? (get-namespace (buffer/buffer ""))))
    (is (= (get-namespace (buffer/buffer "(ns abc.def)\n  asdf")) "abc.def"))
    (is (= (get-namespace (buffer/buffer "(ns abc.def\n  asdf)")) "abc.def"))))

