(ns tiesql.common-test
  (:use [clojure.test])
  (:require [tiesql.common :refer :all]
            [tiesql.core-util :refer :all]))



;(replace-last-in-vector-test)


#_(deftest test-unique-id
    (testing "testing unique id"
      (let)))

(defn meta-fn [a] a)

#_(deftest get-meta-test
    (testing "test get-meta"
      (let [actual-result (get-meta meta-fn)]
        ;     (println actual-result)
        (is (not (nil? actual-result)))
        )))

;(get-meta-test)






;(acc-with-range-tst)





;(merge [2 3 ] [5])

;(concat-distinct-with-range-test)

;(select-values-test)



;(comp-xf-until-test)


;(run-tests)


