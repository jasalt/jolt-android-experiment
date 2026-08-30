(ns poc.raylib.test-runner
  (:require [clojure.test :as test]
            [poc.raylib.app-test]
            [poc.raylib.diagnostics-test]
            [poc.raylib.gallery-test]
            [poc.raylib.gallery-ui-test]))

(defn -main [& _]
  (let [{:keys [fail error] :as result}
        (test/run-tests 'poc.raylib.app-test
                        'poc.raylib.diagnostics-test
                        'poc.raylib.gallery-test
                        'poc.raylib.gallery-ui-test)]
    (when (pos? (+ fail error))
      (throw (ex-info "Raylib pure tests failed" result)))))
