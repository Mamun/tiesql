(ns dadysql.plugin.params.core
  (:require [dady.common :as cc]
            [dady.fail :as f]
            [dadysql.plugin.join.core :as ji]
            [dadysql.plugin.util :as ccu]))


(defn temp-generator [_]
  -1)


(defn assoc-param-path
  [data root-path param-coll]
  (for [p param-coll
        mp (ccu/get-path data root-path (first p))
        :when (not (get-in data mp))]
    (assoc p 0 mp)))


(defmulti param-paths (fn [input-format _ _] input-format))


(defmethod param-paths
  :dadysql.core/format-nested
  [_ [root-m & child-m] param-m]
  (let [model-name (get root-m :dadysql.core/model)
        rp (ccu/get-path param-m model-name)
        rpp (assoc-param-path param-m rp (:dadysql.core/param root-m))
        cpp (for [c child-m
                  :let [crp (ccu/get-path param-m rp (:dadysql.core/model c))]
                  p (assoc-param-path param-m crp (:dadysql.core/param c))]
              p)]
    (-> []
        (into rpp)
        (into cpp))))


(defmethod param-paths
  :dadysql.core/format-map
  [_ tm-coll param-m]
  (->> (map :dadysql.core/param tm-coll)
       (reduce concat)
       (cc/distinct-with-range 1)
       (assoc-param-path param-m (ccu/empty-path))))



(defn do-param1 [generator path m]
  (condp = (second path)
    :dadysql.core/ref-con
    (let [[_ _ v] path]
      v)
    :dadysql.core/ref-key
    (let [[s _ k] path]
      (->> (cc/replace-last-in-vector s k)
           (get-in m)))
    :dadysql.core/ref-fn-key
    (let [[s _ f k] path]
      (->> (cc/replace-last-in-vector s k)
           (get-in m)
           (f)))
    :dadysql.core/ref-gen
    (let [[_ _ v] path]
      (generator {:dadysql.core/name v}))
    m))




(defn param-exec [rinput param-paths generator]
  (reduce (fn [acc-input path]
            (let [rv (do-param1 generator path acc-input)
                  [src] path]
              (if (f/failed? rv)
                (reduced rv)
                (assoc-in acc-input src rv)))
            ) rinput param-paths))


(defn disptach-input-format [req-m]
  (if (and
        (= :dadysql.core/op-push! (:dadysql.core/op req-m))
        (or (:dadysql.core/group req-m)
            (sequential? (:dadysql.core/name req-m))))
    :dadysql.core/format-nested
    :dadysql.core/format-map))


(defmulti bind-input (fn [req-m _ _] (disptach-input-format req-m)))


(defmethod bind-input :dadysql.core/format-map
  [request-m gen tm-coll]
  (let [input (:dadysql.core/input request-m)
        param-path (param-paths :dadysql.core/format-map tm-coll input)
        input (param-exec input param-path gen)]
    (if (f/failed? input)
      input
      (mapv (fn [m] (assoc m :dadysql.core/input input)) tm-coll))))


(defmethod bind-input :dadysql.core/format-nested
  [request-m gen tm-coll]
  (let [input (:dadysql.core/input request-m)
        param-path (param-paths :dadysql.core/format-map tm-coll input)
        input (f/try-> input
                       (param-exec param-path gen)
                       (ji/do-disjoin (get-in tm-coll [0 :dadysql.core/join])))]
    (if (f/failed? input)
      input
      (mapv (fn [m] (assoc m :dadysql.core/input ((:dadysql.core/model m) input))) tm-coll))))


