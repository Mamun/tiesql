(ns dadysql.compiler.spec-test
  (:use [clojure.test]
        [dadysql.compiler.core])
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [dadysql.compiler.file-reader :as f]))




(deftest tx-prop-spec-test
  (testing "tx-prop spec for valid condtion "
    (let [v [:isolation :serializable :read-only? true]
          r (s/conform :dadysql.compiler.spec/tx-prop v)]
      (is (not= ::s/invalid r))))

  (testing "tx-prop spec for valid condtion "
    (let [v [:isolation :serializable :read-only? 1]
          r (s/conform :dadysql.compiler.spec/tx-prop v)]
      (is (= ::s/invalid r))))
  (testing "tx-prop spec for valid condtion "
    (let [v [:isolation :serializable1 :read-only? true]
          r (s/conform :dadysql.compiler.spec/tx-prop v)]
      (is (= ::s/invalid r)))))


;(tx-prop-spec-test)

(deftest validation-spec-test
  (testing "test validation spec "
    (let [v [[:id :type 'vector? "Id will be sequence"]
             [:id :contain 'int? "Id contain will be Long "]]
          r (s/conform :dadysql.compiler.spec/validation v)]
      (is (not= :clojure.spec/invalid r)))))


;(validation-spec-test)



(deftest spec-test
  (testing "test spec only global  "
    (let [w [{:name         :_global_
              :file-reload  true
              :timeout      3000
              :reserve-name #{:create-ddl :drop-ddl :init-data}}]
          actual-result (s/conform :dadysql.compiler.spec/spec w)]
      (is (not= :clojure.spec/invalid actual-result))))
  (testing "test do-compile "
    (let [module {:doc        "Modify department"
                  :name       [:insert-dept :update-dept :delete-dept]
                  :model      :department
                  :validation [[:id :type 'int? "Id will be Long"]]
                  :sql        "insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name);update department set dept_name=:dept_name, transaction_id=:next_transaction_id  where transaction_id=:transaction_id and id=:id;delete from department where id in (:id);"
                  :extend     {:insert-dept {:params  [[:transaction_id :ref-con 0]
                                                       [:transaction_id :ref-con 0]]
                                             :timeout 30}
                               :update-dept {:params [[:next_transaction_id :ref-fn-key 'inc :transaction_id]]}
                               :delete-dept {:validation [[:id :type 'vector? "Id will be sequence"]
                                                          [:id :contain 'int? "Id contain will be Long "]]}}}

          w [module]
          r (s/conform :dadysql.compiler.spec/spec w)]
      (is (not= :clojure.spec/invalid r))))
  (testing "test do-compile "
    (let [config {:name         :_config_
                  :file-reload  true
                  :timeout      3000
                  :reserve-name #{:create-ddl :drop-ddl :init-data}}
          module {:doc        "Modify department"
                  :name       [:insert-dept :update-dept :delete-dept]
                  :model      :department
                  :validation [[:id :type 'int? "Id will be Long"]]
                  :sql        "insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name);update department set dept_name=:dept_name, transaction_id=:next_transaction_id  where transaction_id=:transaction_id and id=:id;delete from department where id in (:id);"
                  :extend     {:insert-dept {:params  [[:transaction_id :ref-con 0]
                                                       [:transaction_id :ref-con 0]]
                                             :timeout 30}
                               :update-dept {:params [[:next_transaction_id :ref-fn-key 'inc :transaction_id]]}
                               :delete-dept {:validation [[:id :type 'vector? "Id will be sequence"]
                                                          [:id :contain 'int? "Id contain will be Long "]]}}}

          w [config module]
          r (s/conform :dadysql.compiler.spec/spec w)]
      (is (not= :clojure.spec/invalid r))))
  )



(deftest spec-test2
  (testing "test do compile file "
    (let [w (-> "tie.edn.sql"
                (f/tie-file-reader)
                (f/map-sql-tag))
          actual-result (s/conform :dadysql.compiler.spec/spec w)]
     ; (clojure.pprint/pprint actual-result)
      (is (not= :clojure.spec/invalid actual-result)))))


;(spec-test2)







;(run-tests)


;(spec-file-test)





#_(->> "tie.edn.sql"
       (f/tie-file-reader)
       (f/map-sql-tag)
       (s/conform :dadysql.compiler.spec/spec)
       (as-map)
       )


#_(do-compile-file-test)









