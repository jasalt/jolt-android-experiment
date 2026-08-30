(ns poc.raylib.test-runner
  (:require [clojure.test :as test]
            [poc.raylib.app-test]
            [poc.raylib.diagnostics-test]
            [poc.raylib.flappy-bird-test]
            [poc.raylib.following-eyes-test]
            [poc.raylib.gallery-test]
            [poc.raylib.gallery-ui-test]
            [poc.raylib.repl-queue-test]))

(defn -main [& _]
  (let [{:keys [fail error] :as result}
        (test/run-tests 'poc.raylib.app-test
                        'poc.raylib.diagnostics-test
                        'poc.raylib.flappy-bird-test
                        'poc.raylib.following-eyes-test
                        'poc.raylib.gallery-test
                        'poc.raylib.gallery-ui-test
                        'poc.raylib.repl-queue-test)]
    (when (pos? (+ fail error))
      (throw (ex-info "Raylib pure tests failed" result)))))
