(ns poc.reducer-test
  (:require [clojure.test :refer [deftest is testing]]
            [poc.reducer :as sut]))

(deftest counter-events
  (testing "increment, decrement, and reset preserve the no-effect contract"
    (is (= [{:counter 1 :events [] :platform nil} []]
           (sut/step sut/initial-state {:type :counter/inc})))
    (is (= [{:counter -1 :events [] :platform nil} []]
           (sut/step sut/initial-state {:type :counter/dec})))
    (is (= [{:counter 0 :events [] :platform nil} []]
           (sut/step (assoc sut/initial-state :counter 9) {:type :counter/reset})))))

(deftest platform-info-event
  (is (= [{:counter 0 :events [] :platform {:name "CLI"}} []]
         (sut/step sut/initial-state {:type :platform/info :value {:name "CLI"}}))))

(deftest unknown-event-is-a-no-op
  (is (= [sut/initial-state []]
         (sut/step sut/initial-state {:type :unknown/event}))))
