(ns dadysql.jdbc
  (:import [java.util.Date]
           [java.util.concurrent.TimeUnit])
  (:require
    [clojure.tools.logging :as log]
    [clojure.spec :as sp]
    [clojure.java.jdbc :as jdbc]
    [dadysql.spec :as tc]
    [dadysql.core :as dc]
    [dadysql.jdbc-core :as tie]
    [dadysql.compiler.core :as fr]
    [dadysql.plugin.factory :as imp]
    [dady.proto :as c]
    [dady.fail :as f]
    [dadysql.plugin.sql.sql-executor :as ce]
    [dadysql.plugin.params.core :as p]))


(defn read-file
  ([file-name] (read-file file-name (imp/new-root-node)))
  ([file-name pc]
   (-> (fr/read-file file-name)
       (assoc-in [tc/global-key :dadysql.spec/file-name] file-name)
       (assoc-in [tc/global-key tc/process-context-key] pc))))



(defn- filter-processor
  [process {:keys [out-format]}]
  (if (= out-format tc/value-format)
    (c/remove-type process :output)
    process))


(defn select-pull-node [ds tms request-m]
  (f/try-> tms
           (get-in [tc/global-key tc/process-context-key] [])
           (filter-processor request-m)
           (c/add-child-one (ce/sql-executor-node ds tms ce/Parallel))))



(defn select-push-node [gen-pull-fn ds tms]
  (f/try-> tms
           (get-in [tc/global-key tc/process-context-key] [])
           (c/remove-type :output)
           (c/add-child-one (ce/sql-executor-node ds tms ce/Transaction))
           (p/assoc-param-ref-gen (fn [& {:as m}]
                                    (->> (dc/default-request :db-seq m)
                                         (gen-pull-fn ds tms))))))


(defn do-run
  [proc tms {:keys [params] :as p}]
  (let [tm-coll (dc/select-name tms p)]
    (if (f/failed? tm-coll)
      tm-coll
      (proc tm-coll params))))



(defn pull
  "Read or query value from database. It will return as model map
   ds: datasource
   "
  [ds tms req-m]
  (let [r (f/try->> req-m
                    (dc/validate-input!)
                    (dc/default-request :pull))]
    (if (f/failed? r)
      r
      (f/try-> (select-pull-node ds tms r)
               (tie/get-process r)
               (do-run tms r)))))



(defn push!
  "Create, update or delete value in database. DB O/P will be run within transaction. "
  [ds tms req-m]
  (let [r (f/try->> req-m
                    (dc/validate-input!)
                    (dc/default-request :push))]
    (if (f/failed? r)
      r
      (f/try-> (select-push-node pull ds tms)
               (tie/get-process r)
               (do-run tms r)))))



(defn db-do [ds name-coll tms]
  (when name-coll
    (try
      (let [tm-coll (vals (dc/select-name-by-name-coll tms name-coll))]
        (doseq [m tm-coll]
          (when-let [sql (get-in m [:dadysql.spec/sql])]
            (log/info "db do with " sql)
            (jdbc/db-do-commands ds sql))))
      (catch Exception e
        (do
          (log/error e)
          (f/fail {:detail e})))))
  tms)



(defn has-dml-type? [m-map]
  (let [dml (:dadysql.spec/dml-key m-map)]
    (or
      (= :dadysql.spec/dml-update dml)
      (= :dadysql.spec/dml-call dml)
      (= :dadysql.spec/dml-insert dml)
      (= :dadysql.spec/dml-delete dml)
      (= :dadysql.spec/dml-select dml))))


(defn get-dml
  [tms]
  (let [p (comp (filter has-dml-type?)
                (map :dadysql.spec/sql)
                (filter (fn [v] (if (< 1 (count v))
                                  true false)))
                (map first)
                (map #(clojure.string/replace % #":\w+" "?")))]
    (into [] p (vals tms))))


(defn validate-dml! [ds tms]
  (let [str-coll (get-dml tms)]
    (jdbc/with-db-connection
      [conn ds]
      (doseq [str str-coll]
        (jdbc/prepare-statement (:connection conn) str)))
    (log/info (format "checking %d dml statement is done " (count str-coll)))

    ;(validate-dml! ds (get-dml tms))
    tms))


(defn start-tracking
  [name callback]
  (ce/start-tracking name callback))


(defn stop-tracking
  [name]
  (ce/stop-tracking name))


(defn- as-date [milliseconds]
  (if milliseconds
    (java.util.Date. milliseconds)))


(defn- execution-log
  [tm-coll]
  (let [v (mapv #(select-keys % [:dadysql.spec/sql :dadysql.spec/exec-total-time :dadysql.spec/exec-start-time]) tm-coll)
        w (mapv (fn [t]
                  (update-in t [:dadysql.spec/exec-start-time] (fn [o] (str (as-date o))))
                  ) v)]
    (log/info w)))


(defn start-sql-execution-log
  "Start sql execution log with sql statement, total duration and time"
  []
  (start-tracking :_sql-execution_ execution-log))


(defn stop-sql-execution-log
  "Stop sql execution log "
  []
  (stop-tracking :_sql-execution_))
