(ns dadysql.client
  (:require [ajax.core :as a]
            [dadysql.util :as u]))


#_(defn default-dadysql-params
    []
    {:input  :keyword
     :output :keyword
     :accept "application/transit+json"})


(defn default-ajax-params
  [ajax-m]
  (-> (merge {:method          :post
              :headers         {}
              :format          (a/transit-request-format)
              :response-format (a/transit-response-format)
              :handler         (fn [v] (js/console.log (str v)))
              :error-handler   (fn [v] (js/console.log (str v)))}
             ajax-m)
      #_(update-in [:params] (fn [v] (merge (default-dadysql-params) v)))))


(defn build-ajax-request
  [params]
  {:params        params
   :handler       (fn [v] (js/console.log (str v)))
   :error-handler (fn [v] (js/console.log (str v)))})


(def csrf-headers {"Accept" "application/transit+json"
                   ;"x-csrf-token" (.-value (.getElementById js/document "csrf-token"))
                   })


(defn pull
  ([url ajax-m]
   (->> (default-ajax-params ajax-m)
        (a/POST (str (or url "") "/pull"))))
  ([ajax-m]
   (pull "" ajax-m)))


(defn push!
  ([url ajax-m]
   (->> (default-ajax-params ajax-m)
        (a/POST (str (or url "") "/push"))))
  ([ajax-m]
   (push! "" ajax-m)))



(comment

  (->> (default-dadysql-params {:a 3})
       (default-dadysql-params)
       (a/POST "/pull")
       )
  )


#_(:cljs
   (defn build-js-request
     [name params options callback]
     (let [namev (mapv keyword (js->clj name))
           params (js->clj params)
           options (js->clj options)
           cb (fn [res] (if (= "text/html" (get-in options [:accept]))
                          (callback res)
                          (callback (clj->js res))))]
       (merge {:name     namev
               :params   params
               :input    :string
               :output   :string
               :callback cb} options))))


#_(def ^:export h-options (clj->js accept-html-options))


