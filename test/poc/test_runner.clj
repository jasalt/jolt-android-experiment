(ns poc.test-runner
  (:require [clojure.test :as test]
            poc.reducer-test))

(defn -main [& _]
  (let [{:keys [fail error]} (test/run-tests 'poc.reducer-test)]
    (System/exit (if (zero? (+ fail error)) 0 1))))
