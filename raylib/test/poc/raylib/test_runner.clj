(ns poc.raylib.test-runner
  (:require [clojure.test :as test]
            [poc.raylib.diagnostics-test]))

(defn -main [& _]
  (let [{:keys [fail error] :as result}
        (test/run-tests 'poc.raylib.diagnostics-test)]
    (when (pos? (+ fail error))
      (throw (ex-info "Raylib pure tests failed" result)))))
