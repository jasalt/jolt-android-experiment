(ns poc.raylib.touch-diagnostics-test (:require [clojure.test :refer [deftest is]] [poc.raylib.touch-diagnostics :as d]))
(deftest scalar-touch-diagnostic-is-honest
 (is (= {:count 2 :ids [3 7] :point-0 [10 20] :coordinates :point-0 :all-coordinates-available? false :phase :down}
        (d/view {:touches {:count 2 :ids [3 7] :point-0 [10 20] :available-coordinates :point-0 :all-coordinates-available? false} :pointer {:phase :down}}))))
