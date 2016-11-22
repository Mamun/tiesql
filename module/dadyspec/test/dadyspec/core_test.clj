(ns dadyspec.core-test
  (:use [clojure.test]
        [dadyspec.core])
  (:require [clojure.spec.test :as stest]
            [clojure.spec :as s]
            [cheshire.core :as ch]
            [clojure.spec.gen :as gen])
  (:import (java.util Date UUID)))


(comment

  (run-tests)
  )

#_(deftest relation-merge-test
  (testing "testing relation merge"
    (let [w (relation-merge :hello [[:a :ta :dadyspec.core/rel-1-1 :b :tb]
                                    [:a :ta :dadyspec.core/rel-1-1 :c :tc]])]
      (is (= w
             [`(clojure.spec/merge
                 :hello/a
                 (clojure.spec/keys :opt [:hello/b-list :hello/c-list]))])))))


(deftest gen-spec-test
  (testing "gen spec test "
    (let [v (gen-spec :app '{:dept    {:req {:name string?
                                             :date inst?}
                                       :opt {:note string?}}
                             :student {:req {:name string?
                                             :dob  inst?}}}
                      {:dadyspec.core/join [[:dept :id :dadyspec.core/rel-1-1 :student :dept-id]]
                       :dadyspec.core/gen-type #{:dadyspec.core/un-qualified}
                       })]
      (is (not-empty v)))))


(deftest check-exec-test
  (testing "test generate spec "
    (do
      (defentity test {:dept {:req {:id int?}
                             :opt  {:note string?}}
                   :student  {:req {:name string?
                                   :id   int?}}}
                 :dadyspec.core/join [[:dept :id :dadyspec.core/rel-1-1 :student :dept-id]]
                 :dadyspec.core/gen-type #{:dadyspec.core/un-qualified
                                           :dadyspec.core/qualified
                                           :dadyspec.core/ex})
      (is (s/valid? :test/dept {:test.dept/id 123}))
      (is (s/valid? :test/dept {:test.dept/id      123
                                :test/student-list [{:test.student/id   23
                                                     :test.student/name "asdf"}]}))
      )))


(deftest do-join-test
  (testing "testing do join "
    (let [w (->> {:dept
                  {:id -1,
                   :name "",
                   :note "",
                   :student-list
                   [{:name "", :id -1}
                    {:name "", :id -1}]}})
          expected-result {:dept
                           {:id -1,
                            :name "",
                            :note "",
                            :student-list
                            [{:name "", :id -1, :dept-id -1} {:name "", :id -1, :dept-id -1}]}}
          j-value  (do-disjoin [[:dept :id :dadyspec.core/rel-1-n :student :dept-id]] w)
          dj-value (do-join [[:dept :id :dadyspec.core/rel-1-n :student :dept-id]] j-value)]
      (is (= expected-result dj-value)))))


(deftest do-dis-join-test
  (testing "testing do disjoin "
    (let [w (->> {:dept
                  {:id -1,
                   :name "",
                   :note "",
                   :student-list
                   [{:name "", :id -1}
                    {:name "", :id -1}]}})
          j-value  (do-disjoin [[:dept :id :dadyspec.core/rel-1-n :student :dept-id]] w)
          expected-value {:dept {:id -1, :name "", :note ""},
                          :student [{:name "", :id -1, :dept-id -1}
                                    {:name "", :id -1, :dept-id -1}]}]
      (is (= expected-value j-value)))))




