(ns poc.reducer-test
  (:require [clojure.test :refer [deftest is testing]]
            [poc.reducer :as sut]))

(deftest counter-events
  (testing "counter changes emit declarative persistence effects"
    (is (= [{:counter 1 :events [] :platform nil :lifecycle nil}
            [{:type :storage/write :key "counter" :value 1}]]
           (sut/step sut/initial-state {:type :counter/inc})))
    (is (= [{:counter -1 :events [] :platform nil :lifecycle nil}
            [{:type :storage/write :key "counter" :value -1}]]
           (sut/step sut/initial-state {:type :counter/dec})))
    (is (= [{:counter 0 :events [] :platform nil :lifecycle nil}
            [{:type :storage/write :key "counter" :value 0}]]
           (sut/step (assoc sut/initial-state :counter 9) {:type :counter/reset})))))

(deftest storage-restoration
  (is (= [{:counter 7 :events [] :platform nil :lifecycle nil} []]
         (sut/step sut/initial-state {:type :storage/restore :value 7}))))

(deftest platform-info-event
  (is (= [{:counter 0 :events [] :platform {:name "CLI"} :lifecycle nil} []]
         (sut/step sut/initial-state {:type :platform/info :value {:name "CLI"}}))))

(deftest platform-effects-are-data
  (is (= [sut/initial-state [{:type :platform/clipboard :text "Jolt counter: 0"}]]
         (sut/step sut/initial-state {:type :platform/copy-counter}))))

(deftest lifecycle-events-and-unknown-events
  (is (= [{:counter 0 :events [] :platform nil :lifecycle :created} []]
         (sut/step sut/initial-state {:type :lifecycle/create})))
  (is (= [{:counter 0 :events [] :platform nil :lifecycle :resumed} []]
         (sut/step sut/initial-state {:type :lifecycle/resume})))
  (is (= [sut/initial-state []]
         (sut/step sut/initial-state {:type :unknown/event}))))
