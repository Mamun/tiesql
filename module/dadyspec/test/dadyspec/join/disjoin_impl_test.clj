(ns dadyspec.join.disjoin-impl-test
  (:use [clojure.test])
  (:require [dadyspec.join.disjoin-impl :refer :all]
            [dadyspec.core :as dc]
            [clojure.spec :as s]
            [dadyspec.join.util :as p]
            [clojure.spec.gen :as g]
            ))


(comment

  (run-tests)
  )

(deftest sdf
  (testing "group-by-target-entity-one test"
    (is (= {:ntab [{:tab1-id 100, :tab-id 10}]}
          (group-by-target-entity-one
            {:tab {:id 100,
                   :tab1-list [{:tab1-id 10}]}}
            [[:tab] :id :dadyspec.core/rel-n-n
             [:tab :tab1-list 0]        :tab1-id
             [:ntab :tab1-id :tab-id]])))))



(deftest group-by-target-entity-one-test
  (testing "test dest-rel-data "
    (let [data {:tab {:id   100
                      :tab1 [{:tab-id 100} {:tab-id 100}]}}
          j [[:tab] :id :dadyspec.core/rel-1-n :tab1 :tab-id]
          expected-result {:tab1 [{:tab-id 100} {:tab-id 100}]}
          actual-result (group-by-target-entity-one data j)]
      (is (= actual-result
             expected-result))))
  (testing "test dest-rel-data with single join "
    (let [data {:tab [{:id   100
                       :tab1 [{:tab-id 100}
                              {:tab-id 100}]}
                      {:id   102
                       :tab1 {:tab-id 138}}]}
          j [[:tab 0] :id :dadyspec.core/rel-n-n [:tab 0 :tab1 0] :tab-id [:ntab :tab-id :tab1-id]]
          expected-result {:ntab [{:tab-id 100, :tab1-id 100}]}
          actual-result (group-by-target-entity-one data j)]
      (is (= actual-result
             expected-result)))))


(deftest assoc-target-entity-key-test
  (testing "test acc-ref-key "
    (let [r [[:tab :id :dadyspec.core/rel-1-n :tab1 :tab-id]]
          ;[[:tab] :id :dadyspec.core/rel-1-n [:tab :tab1 0] :tab-id]
          data {:tab {:id 100, :tab1-list [{:id 101}]}}
          expected-result {:tab1-list [{:id 101, :tab-id 100}]}
          r (-> (p/rename-join-key r)
                (p/replace-source-entity-path data))
          atual-result (assoc-1-join-key data r)]
      (is (= atual-result
             expected-result))))
  (testing "test assoc-target-entity-key "
    (let [r [[:employee :id :dadyspec.core/rel-1-1 :employee-detail :employee_id]]
          data {:employee {:firstname       "Schwan",
                           :lastname        "Ragg",
                           :dept_id         1,
                           :transaction_id  0,
                           :id              109
                           :employee-detail {:street  "Schwan",
                                             :city    "Munich",
                                             :state   "Bayern",
                                             :country "Germany"}}}
          e-result {:employee-detail
                    [{:street "Schwan",
                      :city "Munich",
                      :state "Bayern",
                      :country "Germany",
                      :employee_id 109}]}
          r (-> (p/rename-join-key r)
                (p/replace-source-entity-path data))
          actual-result (assoc-1-join-key data r)]
      (is (= e-result actual-result)))))




(deftest assoc-join-key-test
  (testing "assoc-join-key "
    (let [join [[:tab :id :dadyspec.core/rel-1-n :tab1 :tab-id]]
          data {:tab {:id        100
                      :tab1-list [{:tab-id 100 :name "name1"}
                                  {:tab-id 100 :name "name2"}]}}
          expected-result {:tab {:id 100, :tab1-list [{:tab-id 100, :name "name1"}
                                                      {:tab-id 100, :name "name2"}]}}
          actual-result (assoc-join-key data join)]
      (is (= actual-result expected-result))

      )
    )
  )

;(assoc-join-key-test)


(deftest do-disjoin-test
  (testing "test do-disjoin with :dadyspec.core/rel-1-n relationship "
    (let [join [[:tab :id :dadyspec.core/rel-1-n :tab1 :tab-id]]
          data {:tab {:id   100
                      :tab1-list [{:tab-id 100 :name "name1"}
                             {:tab-id 100 :name "name2"}]}}
          expected-result {:tab  {:id 100}
                           :tab1 [{:tab-id 100 :name "name1"}
                                  {:tab-id 100 :name "name2"}]}
          actual-result (do-disjoin (assoc-join-key data join) join)]
      (is (= actual-result
             expected-result))))
  (testing "test do-disjoin with :n-n relationship "
    (let [join [[:tab :id :dadyspec.core/rel-n-n :tab1 :tab-id [:ntab :tab-id :tab1-id]]]
          data {:tab {:id   100
                      :tab1-list [{:tab-id 100}
                                  {:tab-id 101}]}}
          expected-result {:tab {:id 100},
                           :ntab [{:tab1-id 100, :tab-id 100}
                                  {:tab1-id 100, :tab-id 101}],
                           :tab1 [{:tab-id 100} {:tab-id 100}]}


          actual-result (do-disjoin (assoc-join-key data join) join)
          ]

      ;(assoc-join-key data join)
      (is (= actual-result
             expected-result)))
    )
  (testing "test do-join"
    (let [j [[:employee :id :dadyspec.core/rel-1-1 :employee-detail :employee_id]]
          data {:employee {:firstname       "Schwan",
                           :lastname        "Ragg",
                           :dept_id         1,
                           :transaction_id  0,
                           :id              109
                           :employee-detail {:street  "Schwan",
                                             :city    "Munich",
                                             :state   "Bayern",
                                             :country "Germany"}}}
          expected-result {:employee
                           {:firstname      "Schwan",
                            :lastname       "Ragg",
                            :dept_id        1,
                            :transaction_id 0,
                            :id             109},
                           :employee-detail
                           {:street      "Schwan",
                            :city        "Munich",
                            :state       "Bayern",
                            :country     "Germany",
                            :employee_id 109}

                           }
          actual-result (do-disjoin (assoc-join-key data j) j)]
      (is (= expected-result actual-result)))))

