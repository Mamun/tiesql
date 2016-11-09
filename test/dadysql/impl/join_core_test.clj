(ns dadysql.impl.join-core-test
  (:use [clojure.test])
  (:require [dadysql.impl.join-impl :refer :all]))


(comment

  (run-tests)

  )

(deftest get-source-relational-key-value-test
  (testing "test get-relational-key-value"
    (let [join [[:tab :id :1-n :tab2 :tab-id]
                [:tab :id :1-n :tab3 :tab-id]]
          data-m {:tab [{:id 3 :name "yourname"}
                        {:id 4 :name "yourname"}]}
          expected-result {:tab-id (list 3 4)}
          actual-result (get-source-relational-key-value join data-m)]
      (is (= expected-result
             actual-result)))))

;(group-by-source-entity-key-test)

(deftest group-by-target-entity-key-batch-test
  (testing "test group-by-target-entity-key-batch"
    (let [join [[:tab :id :1-n :tab2 :tab-id]
                [:tab :id :1-n :tab3 :tab-id]]
          data-m {:tab  [{:id 1 :desc "tab "}
                         {:id 2 :desc "tab "}]
                  :tab2 [{:id 3 :tab-id 1 :desc "tab2 "}
                         {:id 4 :tab-id 2 :desc "tab2 "}]
                  :tab3 {:id 1 :tab-id 1 :desc "tab3 "}}
          expected-result {:tab2 {:tab-id {1 [{:id 3, :tab-id 1, :desc "tab2 "}]
                                           2 [{:id 4 :tab-id 2 :desc "tab2 "}]}},
                           :tab3 {:tab-id {1 {:id 1, :tab-id 1, :desc "tab3 "}}}}
          actual-result (group-by-target-entity-key-batch join data-m)]
      (is (= expected-result
             actual-result))
      )))

;(group-by-target-entity-key-batch-test)


(deftest assoc-to-source-entity-batch-test
  (testing "test get-target-relational-key-value"
    (let [data-m {:tab  [{:id 1 :desc "tab "}
                         {:id 2 :desc "tab "}]
                  :tab2 [{:id 3 :tab-id 1 :desc "tab2 "}
                         {:id 4 :tab-id 2 :desc "tab2 "}]
                  :tab3 {:id 1 :tab-id 1 :desc "tab3 "}}

          join (list [[:tab 0] :id :1-n :tab2 :tab-id]
                     [[:tab 1] :id :1-n :tab2 :tab-id]
                     [[:tab 0] :id :1-n :tab3 :tab-id]
                     [[:tab 1] :id :1-n :tab3 :tab-id])

          target-g-m {:tab2 {:tab-id {1 [{:id 3, :tab-id 1, :desc "tab2 "}]
                                      2 [{:id 4 :tab-id 2 :desc "tab2 "}]}},
                      :tab3 {:tab-id {1 {:id 1, :tab-id 1, :desc "tab3 "}}}}
          expected-result {:tab [{:id   1,
                                  :desc "tab ",
                                  :tab2 [{:id 3, :tab-id 1, :desc "tab2 "}],
                                  :tab3 {:id 1, :tab-id 1, :desc "tab3 "}}
                                 {:id   2,
                                  :desc "tab ",
                                  :tab2 [{:id 4, :tab-id 2, :desc "tab2 "}],
                                  :tab3 nil}]}
          actual-result (assoc-to-source-entity-batch target-g-m data-m join)]
      (is (= expected-result (select-keys actual-result [:tab]))))))


;(assoc-to-source-entity-batch-test)


(deftest replace-source-entity-path-test
  (testing "test replace-source-entity-path "
    (let [join [[:tab :id :1-n :tab2 :tab-id]
                [:tab :id :1-n :tab3 :tab-id]]
          data-m {:tab2 [[:tab-id :id] [100 1] [100 2] [1 3]]
                  :tab  [{:id 100 :desc "one "}
                         {:id 100 :desc "two "}]}
          expected-result (list [[:tab 0] :id :1-n :tab2 :tab-id]
                                [[:tab 1] :id :1-n :tab2 :tab-id]
                                [[:tab 0] :id :1-n :tab3 :tab-id]
                                [[:tab 1] :id :1-n :tab3 :tab-id])
          actual-result (replace-source-entity-path join data-m)]
      (is (= actual-result
             expected-result)))))

;(replace-source-entity-path-test)


(deftest replace-target-entity-path-test
  (testing "test replace-target-entity-path "
    (let [join [[[:tab] :id :1-n :tab2 :tab-id]
                [[:tab] :id :1-n :tab3 :tab-id]]
          join-data {:tab {:id   100,
                           :tab2 [{:m "Munich"}]}}
          expected-result (list [[:tab] :id :1-n [:tab :tab2 0] :tab-id])
          actual-result (replace-target-entity-path join join-data)]
      (is (= actual-result
             expected-result))))
  (testing "test replace-target-entity-path "
    (let [join [[[:tab] :id :1-1 :tab2 :tab-id]]
          join-data {:tab {:id   100,
                           :tab2 {:m "Munich"}}}
          expected-result (list [[:tab] :id :1-1 [:tab :tab2] :tab-id])
          actual-result (replace-target-entity-path join join-data)]
      (is (= actual-result
             expected-result)))))


;(replace-target-entity-path-test)


(deftest group-by-target-entity-key-batch-test
  (testing "test group-by-target-entity-key-batch "
    (let [join [[:tab :id :1-n :tab2 :tab-id]
                [:tab :id :1-n :tab3 :tab-id]]
          data {:tab2 [[:tab-id :id] [100 1] [100 2] [1 3]]
                :tab  [{:id 100}]}
          expected-result {:tab2 {:tab-id
                                  {100 [[:tab-id :id]
                                        [100 1]
                                        [100 2]],
                                   1   [[:tab-id :id]
                                        [1 3]]}},
                           :tab3 {:tab-id nil}}
          actual-result (group-by-target-entity-key-batch join data)]
      (is (= actual-result
             expected-result)))))



;(group-by-join-coll-test)

(deftest do-join-test
  (testing "test do-join "
    (let [join [[:tab :id :dadysql.core/join-one-many :tab2 :tab-id]
                [:tab :id :dadysql.core/join-one-many :tab3 :tab-id]]

          data {:tab2 [[:tab-id :id] [100 1] [100 2] [1 3]]
                :tab  [{:id 100}]}

          expected-result {:tab
                           [{:id   100
                             :tab2 [[:tab-id :id] [100 1] [100 2]]
                             :tab3 nil}]}
          actual-result (do-join (assoc-join-key data join) join)]
      (is (= expected-result actual-result))))
  (testing "test do-join "
    (let [join [[:tab :id :dadysql.core/join-one-one :tab1 :tab-id]
                [:tab :tab4-id :dadysql.core/join-many-one :tab4 :id]
                [:tab :id :dadysql.core/join-many-many :tab2 :id [:tab-tab1 :tab-id :tab2-id]]]

          data {:tab      {:id 100 :tab4-id 1}
                :tab1     {:tab-id 100}
                :tab4     {:id 1}
                :tab-tab1 [{:tab2-id 102 :tab-id 100}
                           {:tab2-id 103 :tab-id 100}]}
          expected-result {:tab
                           {:id      100
                            :tab4-id 1
                            :tab1    {:tab-id 100}
                            :tab4    {:id 1}
                            :tab2    [{:tab2-id 102 :tab-id 100}
                                      {:tab2-id 103 :tab-id 100}]}}
          actual-result (do-join (assoc-join-key data join) join)]
      (is (= actual-result
             expected-result)))))



;(do-join-test)


(deftest assoc-target-entity-key-test
  (testing "test acc-ref-key "
    (let [r [[:tab] :id :1-n [:tab :tab1 0] :tab-id]
          data {:tab {:id 100, :tab1 [{:tab-id 101}]}}
          expected-result {:tab {:id 100, :tab1 [{:tab-id 100}]}}
          atual-result (assoc-target-entity-key data r)]
      (is (= atual-result
             expected-result))))
  (testing "test acc-ref-key "
    (let [r [[:tab] :id :1-n [:tab :tab1 0] :tab-id]
          data {:tab {:id 100}}
          expected-result {:tab {:id 100}}
          atual-result (assoc-target-entity-key data r)]
      (is (= atual-result
             expected-result))))
  (testing "test acc-ref-key with worng target path "
    (let [r [[:tab] :id :1-n [:tab :tab1] :tab-id]
          data {:tab {:id 100, :tab1 [{:tab-id 101}]}}
          expected-result {:tab {:id   100
                                 :tab1 [{:tab-id 101}]}}
          atual-result (assoc-target-entity-key data r)]
      (is (= atual-result
             expected-result))))
  (testing "test acc-ref-key for map target "
    (let [r [[:tab] :id :1-n [:tab :tab1] :tab-id]
          data {:tab {:id 100, :tab1 {:tab-id 101}}}
          expected-result {:tab {:id   100
                                 :tab1 {:tab-id 100}}}
          atual-result (assoc-target-entity-key data r)]
      (is (= atual-result
             expected-result))))
  (testing "test assoc-target-entity-key "
    (let [r [[:employee] :id :1-1 [:employee :employee-detail] :employee_id]
          data {:employee {:firstname       "Schwan",
                           :lastname        "Ragg",
                           :dept_id         1,
                           :transaction_id  0,
                           :id              109
                           :employee-detail {:street  "Schwan",
                                             :city    "Munich",
                                             :state   "Bayern",
                                             :country "Germany"}}}
          atual-result (assoc-target-entity-key data r)
          ]
      (is (= (get-in data [:employee :id])
             (get-in atual-result [:employee :employee-detail :employee_id])
             ))

      ))
  )


;(assoc-target-entity-key-test)

(deftest group-by-target-entity-one-test
  (testing "test dest-rel-data "
    (let [data {:tab {:id   100
                      :tab1 [{:tab-id 100} {:tab-id 100}]}}
          j [[:tab] :id :dadysql.core/join-one-many :tab1 :tab-id]
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
          j [[:tab 0] :id :dadysql.core/join-many-many [:tab 0 :tab1 0] :tab-id [:ntab :tab-id :tab1-id]]
          expected-result {:ntab [{:tab-id 100, :tab1-id 100}]}
          actual-result (group-by-target-entity-one data j)]
      (is (= actual-result
             expected-result)))))


;(dest-rel-data-test)



;(ndest-rel-data-test)


#_(let [join [[:tab :id :dadysql.core/join-one-many :tab1 :tab-id]]
      data {:tab {:id   100
                  :tab1 [{ :name "name1"}
                         { :name "name2"}]}}

      ]
  (clojure.pprint/pprint
    (assoc-join-key data join))

  )

(deftest do-disjoin-test
  (testing "test do-disjoin with :1-n relationship "
    (let [join [[:tab :id :dadysql.core/join-one-many :tab1 :tab-id]]
          data {:tab {:id   100
                      :tab1 [{:tab-id 100 :name "name1"}
                             {:tab-id 100 :name "name2"}]}}
          expected-result {:tab  {:id 100}
                           :tab1 [{:tab-id 100 :name "name1"}
                                  {:tab-id 100 :name "name2"}]}
          actual-result (do-disjoin (assoc-join-key data join) join)]
      (is (= actual-result
             expected-result))))
  (testing "test do-disjoin with :1-n relationship "
    (let [join [[:tab :id :dadysql.core/join-one-many :tab1 :tab-id]]
          data {:tab [{:id   100
                       :tab1 [{:tab-id 100 :name "name1"}
                              {:tab-id 100 :name "name2"}]}
                      {:id   100
                       :tab1 {:tab-id 138 :name "name3"}}]}
          expected-result {:tab  [{:id 100} {:id 100}]
                           :tab1 (list {:tab-id 100 :name "name1"}
                                       {:tab-id 100 :name "name2"}
                                       {:tab-id 100 :name "name3"})}
          actual-result (do-disjoin (assoc-join-key data join) join)]
      (is (= actual-result
             expected-result))))
  (testing "test do-disjoin with :n-n relationship "
    (let [join [[:tab :id :dadysql.core/join-many-many :tab1 :tab-id [:ntab :tab-id :tab1-id]]]
          data {:tab [{:id   100
                       :tab1 [{:tab-id 100}
                              {:tab-id 101}]}
                      {:id   102
                       :tab1 {:tab-id 138}}]}
          expected-result {:tab  [{:id 100} {:id 102}],
                           :tab1 [{:tab-id 100} {:tab-id 102}]
                           :ntab [{:tab-id 100, :tab1-id 100}
                                  {:tab-id 100, :tab1-id 101}
                                  {:tab-id 102, :tab1-id 138}]}
          actual-result (do-disjoin (assoc-join-key data join) join)]
      (is (= actual-result
             expected-result))))
  (testing "test do-join"
    (let [j [[:employee :id :dadysql.core/join-one-one :employee-detail :employee_id]]
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

(comment

  (run-tests)
  )


;(do-disjoin-test)


;(do-disjoin-test)

;(run-tests)
;(do-disjoin-test-1)

#_(deftest group-by-join-src-test
  (testing "test group-by-join-src "
    (let [v [[:tab :id :dadysql.core/join-one-many :tab1 :tab-id]]
          expected-result {:tab {:dadysql.core/join [[:tab :id :dadysql.core/join-one-many :tab1 :tab-id]]}}
          actual-result (group-by-join-src v)]
      (is (= actual-result
             expected-result)))))

;(group-by-join-src-test)


;(filter-join-key-coll-test)

#_(deftest join-emission-batch-test
  (testing "test join-emission-batch "
    (let [join [[:tab :ID :dadysql.core/join-one-many :tab2 :tab-id]
                [:tab :id :dadysql.core/join-one-many :tab3 :tab-ID]
                [:tab :id :dadysql.core/join-many-many :tab1 :tab-id [:ntab :tab-ID :tab1-id]]]
          expected-result [[:tab :id :dadysql.core/join-one-many :tab2 :tab-id]
                           [:tab :id :dadysql.core/join-one-many :tab3 :tab-id]
                           [:tab :id :dadysql.core/join-many-many :tab1 :tab-id [:ntab :tab-id :tab1-id]]]
          actual-result (join-emission-batch join)]
      (is (= expected-result actual-result)))))

;(join-emission-batch-test)