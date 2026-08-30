(ns poc.reducer-test
  (:require [clojure.test :refer [deftest is testing]]
            [poc.contracts :as contracts]
            [poc.reducer :as sut]
            [poc.wire :as wire]))

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

(deftest portable-capability-contract
  (is (= #{:clipboard :persistence :open-uri :notifications :vibration}
         (contracts/capabilities :android)))
  (is (contracts/permitted-effect? :linux {:type :platform/clipboard}))
  (is (= {:platform :linux
          :capabilities #{:clipboard :persistence :open-uri}
          :capability-status {:open-uri :request-only}}
         (contracts/platform-description :linux {:open-uri :request-only})))
  (is (not (contracts/permitted-effect? :cli {:type :platform/clipboard})))
  (is (= [(assoc base-model :platform (contracts/platform-description :cli))
          [{:type :platform/open-uri :uri "https://example.org"}]]
         (sut/step (assoc sut/initial-state :platform (contracts/platform-description :cli))
                   {:type :platform/request-effect
                    :effect {:type :platform/open-uri :uri "https://example.org"}})))
  (is (= [(assoc base-model :platform (contracts/platform-description :cli)
                         :last-unsupported-effect :platform/clipboard) []]
         (sut/step (assoc sut/initial-state :platform (contracts/platform-description :cli))
                   {:type :platform/request-effect
                    :effect {:type :platform/clipboard :text "x"}}))))

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

(deftest canonical-wire-contract
  (is (= {:ok {:type :counter/inc}}
         (wire/decode-event "{:type :counter/inc}")))
  (is (= :wire/malformed (get-in (wire/decode-event "{") [:error :type])))
  (is (= :wire/invalid (get-in (wire/decode-event "[]") [:error :type])))
  (let [text "Suomi 😀 é"
        encoded (wire/encode-response {:model {:text text} :effects []})]
    (is (= {:ok {:model {:text text} :effects []}} (wire/decode-response encoded))))
  (is (= :wire/too-large
         (get-in (wire/decode-event (apply str (repeat (+ wire/max-input-bytes 1) "a"))) [:error :type]))))

(deftest derived-view-model
  (is (= {:counter 3 :event-count 0 :lifecycle :resumed :worker nil
          :notification-permission nil :platform nil}
         (sut/view-model (assoc sut/initial-state :counter 3 :lifecycle :resumed)))))

(deftest lifecycle-and-worker-events
  (is (= [(assoc base-model :lifecycle :created) []]
         (sut/step sut/initial-state {:type :lifecycle/create})))
  (is (= [(assoc base-model :lifecycle :resumed) []]
         (sut/step sut/initial-state {:type :lifecycle/resume})))
  (is (= [(assoc base-model :lifecycle :paused) []]
         (sut/step sut/initial-state {:type :lifecycle/pause})))
  (is (= [(assoc base-model :lifecycle :stopped) []]
         (sut/step sut/initial-state {:type :lifecycle/stop})))
  (is (= [(assoc base-model :worker :completed) []]
         (sut/step sut/initial-state {:type :worker/completed})))
  (is (= [sut/initial-state []]
         (sut/step sut/initial-state {:type :unknown/event}))))
