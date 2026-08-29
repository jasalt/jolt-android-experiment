(ns poc.reducer-test
  (:require [clojure.test :refer [deftest is testing]]
            [poc.reducer :as sut]))

(def base-model
  {:counter 0 :events [] :platform nil :lifecycle nil :worker nil
   :notification-permission nil})

(deftest counter-events
  (testing "counter changes emit declarative persistence effects"
    (is (= [(assoc base-model :counter 1)
            [{:type :storage/write :key "counter" :value 1}]]
           (sut/step sut/initial-state {:type :counter/inc})))
    (is (= [(assoc base-model :counter -1)
            [{:type :storage/write :key "counter" :value -1}]]
           (sut/step sut/initial-state {:type :counter/dec})))
    (is (= [base-model [{:type :storage/write :key "counter" :value 0}]]
           (sut/step (assoc sut/initial-state :counter 9) {:type :counter/reset})))))

(deftest storage-restoration
  (is (= [(assoc base-model :counter 7) []]
         (sut/step sut/initial-state {:type :storage/restore :value 7}))))

(deftest platform-info-event
  (is (= [(assoc base-model :platform {:name "CLI"}) []]
         (sut/step sut/initial-state {:type :platform/info :value {:name "CLI"}}))))

(deftest platform-effects-are-data
  (is (= [base-model [{:type :platform/clipboard :text "Jolt counter: 0"}]]
         (sut/step sut/initial-state {:type :platform/copy-counter}))))

(deftest permission-events-are-data
  (is (= [base-model [{:type :permission/request :permission :notifications}]]
         (sut/step sut/initial-state {:type :permission/request-notifications})))
  (is (= [(assoc base-model :notification-permission :granted) []]
         (sut/step sut/initial-state {:type :permission/result-granted})))
  (is (= [(assoc base-model :notification-permission :denied) []]
         (sut/step sut/initial-state {:type :permission/result-denied}))))

(deftest reducer-state-remains-portable-data
  (let [[model effects] (sut/step sut/initial-state {:type :counter/inc})]
    (is (= :storage/write (:type (first effects))))
    (is (= {:counter 1
            :events []
            :platform nil
            :lifecycle nil
            :worker nil
            :notification-permission nil}
           model))
    (is (= [{:type :storage/write :key "counter" :value 1}]
           effects))))

(deftest lifecycle-and-worker-events
  (is (= [(assoc base-model :lifecycle :created) []]
         (sut/step sut/initial-state {:type :lifecycle/create})))
  (is (= [(assoc base-model :lifecycle :resumed) []]
         (sut/step sut/initial-state {:type :lifecycle/resume})))
  (is (= [(assoc base-model :worker :completed) []]
         (sut/step sut/initial-state {:type :worker/completed})))
  (is (= [sut/initial-state []]
         (sut/step sut/initial-state {:type :unknown/event}))))
