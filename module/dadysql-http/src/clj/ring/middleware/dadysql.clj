(ns ring.middleware.dadysql
  (:require [clojure.tools.logging :as log]
            [dady.fail :as f]
            [dadysql.http-service :as h]
            [dadysql.jdbc :as tj]))


(defn- reload-tms
  ([tms-atom ds]
   (when (get-in @tms-atom [:_global_ :dadysql.core/file-reload ])
     (f/try->> (get-in @tms-atom [:_global_ :dadysql.core/file-name])
               (tj/read-file)
               (tj/validate-dml! ds)
               (reset! tms-atom)))
   @tms-atom))


(defn try!
  [form & v]
  (try
    (apply form v)
    (catch Exception e
      (log/error e)
      (f/fail {:msg "Error in server "}))))



;ds-atom tms-atom
(defn warp-dadysql-handler
  "Warper that tries to do with dadysql. It should use next to the ring-handler. If path-in is matched with
   pull-path or push-path then it will API and return result.

   handler: Ring handler
   ds-atom: Clojure datasource as atom
   tms-atom: dadysql file as atom
   pull-path and push path string

  "
  [handler & {:keys [pull-path push-path log? tms ds encoding]
              :or   {pull-path "/pull"
                     push-path "/push"
                     }}]
  (fn [req]
    (let [request-path (or (:path-info req)
                           (:uri req))]
      ;(log/info "---request path " request-path)
      (condp = request-path
        pull-path
        (let [ds (or (:ds req) @ds)
              tms (or (:tms req)
                      (try! reload-tms tms ds))
              handler (-> (partial tj/pull ds tms)
                          (h/warp-pull)
                          (h/warp-default ))]
          (handler req))
        push-path
        (let [ds (or (:ds req) @ds)
              tms (or (:tms req)
                      (try! reload-tms tms ds))
              handler (-> (partial tj/push! ds tms)
                          (h/warp-push)
                          (h/warp-default ))]
          (handler req))
        (do
          (handler req))))))



(defn get-sql-file-value [tms-atom]
  (->> @tms-atom
       (vals)
       (mapv (fn [w] (select-keys w [:dadysql.core/name :dadysql.core/model :dadysql.core/sql])))))




