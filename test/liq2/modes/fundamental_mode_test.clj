(ns liq2.modes.fundamental-mode-test
  (:require [clojure.test :refer :all]
            [liq2.buffer :as buffer]
            [liq2.modes.fundamental-mode :refer :all]))

(deftest get-namespace-test
  (testing "Get namespace"
    (is (nil? (get-namespace (buffer/buffer ""))))
    (is (= (get-namespace (buffer/buffer "(ns abc.def)\n  asdf")) "abc.def"))
    (is (= (get-namespace (buffer/buffer "(ns abc.def\n  asdf)")) "abc.def"))))

