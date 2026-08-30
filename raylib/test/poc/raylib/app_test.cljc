(ns poc.raylib.app-test
  (:require [clojure.test :refer [deftest is]]
            [poc.raylib.app :as app]))

(def controls
  [{:action :decrement :x 0 :y 0 :width 100 :height 80}
   {:action :increment :x 110 :y 0 :width 100 :height 80}
   {:action :reset :x 220 :y 0 :width 100 :height 80}])

(defn input [phase point]
  {:pointer {:phase phase :position point}
   :keyboard {:activate? false :previous? false}})

(deftest raylib-controls-use-shared-events-test
  (is (= {:type :counter/inc}
         (app/counter-event :scene (input :press [120 20]) controls)))
  (is (= {:type :counter/dec}
         (app/counter-event :scene (input :press [20 20]) controls)))
  (is (= {:type :counter/reset}
         (app/counter-event :scene (input :press [230 20]) controls)))
  (is (nil? (app/counter-event :gallery (input :press [120 20]) controls))))

(deftest raylib-dispatch-preserves-portable-reducer-effects-test
  (let [[state effects] (app/step {:counter 0 :events [] :platform nil
                                   :lifecycle nil :worker nil
                                   :notification-permission nil}
                                  {:type :counter/inc})]
    (is (= 1 (:counter state)))
    (is (= [{:type :storage/write :key "counter" :value 1}] effects))))
