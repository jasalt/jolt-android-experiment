(ns poc.raylib.repl-queue-test
  (:require [clojure.test :refer [deftest is testing]]
            [poc.raylib.repl-queue :as queue]))

(deftest bounded-fifo-test
  (let [requests (mapv #(hash-map :id % :work identity)
                       (range queue/max-pending))
        full (reduce (fn [state request]
                       (let [[next-state accepted?] (queue/enqueue state request)]
                         (is accepted?)
                         next-state))
                     queue/initial-state requests)
        [unchanged accepted?] (queue/enqueue full {:id :overflow})
        [after-take request] (queue/take-next full)]
    (testing "pending work is bounded and FIFO"
      (is (false? accepted?))
      (is (= full unchanged))
      (is (= 0 (:id request)))
      (is (= (vec (rest requests)) (:pending after-take))))))

(deftest results-are-bounded-test
  (let [completed (reduce (fn [state id]
                            (queue/complete state id {:status :ok :value id}))
                          queue/initial-state
                          (range (inc queue/max-results)))]
    (is (nil? (queue/result completed 0)))
    (is (= {:status :ok :value queue/max-results}
           (queue/result completed queue/max-results)))
    (is (= queue/max-results (count (:result-order completed))))))
