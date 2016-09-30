(ns dadysql.jdbc
  (:import [java.util.Date]
           [java.util.concurrent.TimeUnit])
  (:require
    [clojure.tools.logging :as log]
    [clojure.java.jdbc :as jdbc]
    [dadysql.core-selector :as dc]
    [dadysql.jdbc-core :as tie]
    [dadysql.spec-core :as sc]
    [dadysql.compiler.core :as fr]
    [dadysql.plugin.factory :as imp]
    [dady.proto :as c]
    [dady.fail :as f]
    [dadysql.plugin.sql.jdbc-io :as ce]
    [dadysql.plugin.params.core :as p]))


(defn read-file
  ([file-name] (read-file file-name (imp/new-root-node)))
  ([file-name pc]
   (-> (fr/read-file file-name)
       (assoc-in [:_global_ :dadysql.core/file-name] file-name)
       (assoc-in [:_global_ :dadysql.core/process-context-key] pc))))



(defn- filter-processor
  [process r]
  (if (= (:dadysql.core/output-format r) :dadysql.core/format-value)
    (c/remove-type process :output)
    process))


(defn select-pull-node [ds tms request-m]
  (f/try-> tms
           (get-in [:_global_ :dadysql.core/process-context-key] [])
           (filter-processor request-m)
           (c/add-child-one (ce/sql-executor-node ds tms :dadysql.plugin.sql.jdbc-io/parallel))))



(defn select-push-node [gen-pull-fn ds tms]
  (f/try-> tms
           (get-in [:_global_ :dadysql.core/process-context-key] [])
           (c/remove-type :output)
           (c/add-child-one (ce/sql-executor-node ds tms :dadysql.plugin.sql.jdbc-io/transaction))
           (p/assoc-param-ref-gen (fn [& {:as m}]
                                    (->> (dc/assoc-format :db-seq m)
                                         (gen-pull-fn ds tms))))))



(defn pull
  "Read or query value from database. It will return as model map
   ds: datasource
   "
  [ds tms req-m]
  (if-let [r (f/failed? (sc/validate-input! req-m))]
    r
    (let [req-m (dc/assoc-format :pull req-m)
          node (select-pull-node ds tms req-m)]
      (f/try-> tms
               (dc/select-name req-m)
               (dc/assoc-result-format req-m)
               (tie/do-param node req-m)
               (tie/validate-param-spec!)
               (tie/run-process node req-m)))))



(defn push!
  "Create, update or delete value in database. DB O/P will be run within transaction. "
  [ds tms req-m]
  (if-let [r (f/failed? (sc/validate-input! req-m))]
    r
    (let [req-m (dc/assoc-format :push req-m)
          node (select-push-node pull ds tms)]
      (f/try-> tms
               (dc/select-name req-m)
               (dc/assoc-result-format req-m)
               (tie/do-param node req-m)
               (tie/validate-param-spec!)
               (tie/run-process node req-m)))))



(defn db-do [ds name-coll tms]
  (when name-coll
    (try
      (let [tm-coll (vals (dc/select-name-by-name-coll tms name-coll))]
        (doseq [m tm-coll]
          (when-let [sql (get-in m [:dadysql.core/sql])]
            (log/info "db do with " sql)
            (jdbc/db-do-commands ds sql))))
      (catch Exception e
        (do
          (log/error e)
          (f/fail {:detail e})))))
  tms)



(defn has-dml-type? [m-map]
  (let [dml (:dadysql.core/dml-key m-map)]
    (or
      (= :dadysql.core/dml-update dml)
      (= :dadysql.core/dml-call dml)
      (= :dadysql.core/dml-insert dml)
      (= :dadysql.core/dml-delete dml)
      (= :dadysql.core/dml-select dml))))


(defn get-dml
  [tms]
  (let [p (comp (filter has-dml-type?)
                (map :dadysql.core/sql)
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
  (let [v (mapv #(select-keys % [:dadysql.core/sql :dadysql.core/exec-total-time :dadysql.core/exec-start-time]) tm-coll)
        w (mapv (fn [t]
                  (update-in t [:dadysql.core/exec-start-time] (fn [o] (str (as-date o))))
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
