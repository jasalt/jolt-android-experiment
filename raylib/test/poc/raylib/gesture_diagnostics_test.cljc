(ns poc.raylib.gesture-diagnostics-test (:require [clojure.test :refer [deftest is]] [poc.raylib.gesture-diagnostics :as g]))
(defn input [code] {:gesture {:code code} :pointer {:position [1 2]}})
(deftest gesture-log-is-transition-bounded
 (let [a (g/step {:last-code 0 :log []} (input 1)) b (g/step a (input 1)) c (g/step b (input 0)) d (g/step c (input 1))]
  (is (= ["TAP"] (:log a))) (is (= ["TAP"] (:log b))) (is (= ["TAP" "TAP"] (:log d)))
  (is (= g/max-log (count (:log (reduce (fn [s _] (g/step (g/step s (input 0)) (input 8))) d (range 30))))))))
