(ns walkthrough
  (:require [clojure.java.jdbc :as jdbc]
            [dadysql.jdbc :as t]
            [dadysql.sql-io-impl :as io]
            [test-data :as td]
            [dadysql.compiler.spec :as cs]
            [clojure.spec :as s]))



;(integer? 3)
;(get-in u/tie-system-v [:tms])

(comment


  (cs/gen-spec :hello (vals (t/read-file "tie.edn.sql")))

  #_(s/valid? (s/or :dadysql.core/name string?
                  :id   integer?) :keyowrd)

  ;; Create database table and init data
  (->> {:dadysql.core/name [:create-ddl :init-data]}
       (t/select-name (t/read-file "tie.edn.sql") )
       (io/db-do (td/get-ds) ))



  (-> @td/ds
      (t/db-do  [:drop-ddl] (t/read-file "tie.edn.sql")))

  ;; Validate all sql statment with database
  (-> (t/validate-dml! @td/ds (t/get-all-sql (t/read-file "tie.edn.sql")))
      (clojure.pprint/pprint))


  ;; jdbc query example
  (-> @td/ds
      (jdbc/query ["select * from department "]))


  ;; Simple pull
  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              {:dadysql.core/name :get-dept-list}) )


  ;;With model name when name as vector
  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              {:dadysql.core/name [:get-dept-list]})
      (clojure.pprint/pprint)
      )



  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              {:dadysql.core/name [:get-dept-list :get-employee-list]})
      (clojure.pprint/pprint))


  (-> (t/read-file "tie2.edn.sql")
      (:get-dept-by-id)
      (:dadysql.core/param-spec)
      (s/explain-str {:id 2} )
      )


  ;; with params
  (-> @td/ds
      (t/pull (t/read-file "tie2.edn.sql")
              {:dadysql.core/name  [:get-dept-by-id]
               :dadysql.core/param {:id [1]}}
              ;:dadysql.core/output-format :map
              )
      (clojure.pprint/pprint))


  ;(jdbc/query @td/ds "select * from department where id = 1 ")
  ;(jdbc/execute! @td/ds ["insert into department (id, transaction_id, dept_name) values (9, 0, 'Business' )"])


  ;; for sequence of params

  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              {:dadysql.core/name   [:get-dept-by-ids]
               :dadysql.core/param {:id [ 1 2 3] }})
      (clojure.pprint/pprint))



  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              {:dadysql.core/name   [:get-dept-by-ids]
               :dadysql.core/param {:id [1 2 112]}})
      (clojure.pprint/pprint))


  ;; for join data
  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              {:dadysql.core/name   [:get-dept-by-id :get-dept-employee]
               :dadysql.core/param {:id 1 }})
      (clojure.pprint/pprint))

  (do
    (t/read-file "tie.edn.sql")
    nil
    )


  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              {:dadysql.core/name   [:get-employee-by-id :get-employee-dept :get-employee-detail]
               :dadysql.core/param {:id 1}}
              )
      (clojure.pprint/pprint))


  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              {:dadysql.core/name   [:get-employee-detail]
               :dadysql.core/param {:id 1}}
              ;:dadysql.core/output-format :nested-join
              )
      (clojure.pprint/pprint))



  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              {:dadysql.core/group  :load-employee
               :dadysql.core/param {:id 1}}
              ;:dadysql.core/output-format :nested-join
              )
      (clojure.pprint/pprint))



  ;; Write single
  (let [v {:dept_name "Call Center Munich"}]
    (-> @td/ds
        (t/push! (t/read-file "tie.edn.sql")
                 {:dadysql.core/name   :create-dept
                  :dadysql.core/param v}
                 ;:dadysql.core/output-format :as-sequence
                 )
        (clojure.pprint/pprint)))




  ;; Wirite with batch!
  (let [d {:department {:dept_name "Call Center Munich 2"}}]
    (-> @td/ds
        (t/push! (t/read-file "tie.edn.sql")
                 {:dadysql.core/name   [:create-dept]
                  :dadysql.core/param d}

                 )
        (clojure.pprint/pprint)))




  ;(:insert-dept (t/read-file "tie.edn.sql"))

  (let [d {:dept_name "Call Center Munich 2"}]
    (-> @td/ds
        (t/push! (t/read-file "tie.edn.sql")
                 :dadysql.core/name :insert-dept
                 :params d)
        (clojure.pprint/pprint)))


  ;; Write with batch!
  (let [d {:department [{:dept_name "Software dept "}
                        {:dept_name "Hardware dept"}]}]
    (-> @td/ds
        (t/push! (t/read-file "tie.edn.sql")
                 :dadysql.core/name [:insert-dept]
                 :params d)
        (clojure.pprint/pprint)))



  ;; Check all depts
  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              {:dadysql.core/name [:get-dept-list]})
      (clojure.pprint/pprint))







  ;; for single result
  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              {
               :dadysql.core/name [:gen-dept :gen-empl]}
              ;:dadysql.core/output-format :map
              )
      (clojure.pprint/pprint))


  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              :dadysql.core/name :gen-dept
              :dadysql.core/output-format value-format)
      (clojure.pprint/pprint))

  ;; Update department name
  (let [d {:department {:dept_name "Call Center Munich 1" :transaction_id 0 :id 1}}]
    (-> @td/ds
        (t/push! (t/read-file "tie.edn.sql")
                 {
                  :dadysql.core/name [:update-dept]
                  :dadysql.core/param            d})
        (clojure.pprint/pprint)))

  (let [d {:dept_name "Call Center Munich 1" :transaction_id 0 :id 2}]
    (-> @td/ds
        (t/push! (t/read-file "tie.edn.sql")
                 {
                  :dadysql.core/name   :update-dept
                  :params d})
        (clojure.pprint/pprint)))


  ;; Delete department
  (let [input {:department {:id [101 102]}}]
    (-> @td/ds
        (t/push! (t/read-file "tie.edn.sql")
                 :dadysql.core/name [:delete-dept]
                 :params input)
        (clojure.pprint/pprint)))



  ;;;;;;;;;;;;; Employee  ###############################################################

  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              :dadysql.core/name [:get-employee-list])
      (clojure.pprint/pprint))


  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              :dadysql.core/name :get-employee-by-id
              :params {:id 1}
              :dadysql.core/output-format :array
              )
      (clojure.pprint/pprint))


  ;; Write join data
  (let [employee {:employee {:firstname       "Schwan"
                             :lastname        "Ragg"
                             :dept_id         1
                             :employee-detail {:street  "Schwan",
                                               :city    "Munich",
                                               :state   "Bayern",
                                               :country "Germany"}}}]
    (-> @td/ds
        (t/push! (t/read-file "tie.edn.sql")
                 :dadysql.core/name [:create-employee :create-employee-detail]
                 :params employee
                 ;:iormat as-model
                 ;:dadysql.core/output-format as-model
                 )
        (clojure.pprint/pprint)
        )
    )




  ;;;;;######################## Meeting [Employee n:n Meeting] ############


  ;; read all meeting
  (-> {:datasource @conn}
      (t/pull (t/read-file "tie.edn.sql")
              {:dadysql.core/name [:get-meeting-list]})
      (clojure.pprint/pprint))


  ;; add new meeting
  (let [meeting {:meeting
                 {:subject "Hello Meeting for IT"
                  }}]
    (->
      @td/ds
      (t/push! (t/read-file "tie.edn.sql")
               {:dadysql.core/name   [:create-meeting]
                :params meeting})
      (clojure.pprint/pprint)
      )
    )

  ;; join with n-n
  (let [meeting {:meeting {:subject  "Hello Meeting for Manager"
                           :employee {:id 112}}}]
    (->
      (t/read-file "tie.edn.sql")
      (t/push! @td/ds
               {:dadysql.core/name   [:create-meeting :create-employee-meeting]
                :params meeting})
      (clojure.pprint/pprint)
      )
    )


  (let [meeting {:meeting {:subject  "Hello Meeting for IT"
                           :employee [{:current_transaction_id 1,
                                       :dept_id                2,
                                       :lastname               "Zoma",
                                       :firstname              "Abba"
                                       :id                     1}
                                      {:current_transaction_id 1,
                                       :dept_id                2,
                                       :lastname               "Zoma",
                                       :firstname              "Abba"
                                       :id                     2}]}}]
    (-> (t/read-file "tie.edn.sql")
        (t/push! @td/ds
                 :dadysql.core/name [:create-meeting :create-employee-meeting]
                 :params meeting)
        (clojure.pprint/pprint)))



  ;;;; Check sql tracking

  #_(t/start-tracking :hello
                    (fn [v]
                      (clojure.pprint/pprint v)))

  #_(t/stop-tracking :hello)

  #_(t/start-sql-execution)






  )
