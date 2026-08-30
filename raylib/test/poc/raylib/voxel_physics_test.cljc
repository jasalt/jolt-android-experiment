(ns poc.raylib.voxel-physics-test
  (:require [clojure.test :refer [deftest is]]
            [poc.raylib.voxel-physics :as physics]
            [poc.raylib.voxel-siege :as siege]))

(defn callbacks [calls]
  {:world-create (fn [& _] (swap! calls conj :create) 42)
   :world-step (fn [world dt substeps]
                 (swap! calls conj [:step world dt substeps]))
   :world-destroy (fn [world] (swap! calls conj [:destroy world]))})

(deftest owner-affine-lifecycle
  (let [calls (atom [])
        adapter (physics/init (callbacks calls))
        [stepped facts] (physics/step adapter 1.0)
        disposed (physics/dispose stepped)]
    (is (= :active (:status adapter)))
    (is (= 42 (:world-id adapter)))
    (is (= 0.033 (:dt facts)))
    (is (= :disposed (:status disposed)))
    (is (= disposed (physics/dispose disposed)))
    (is (= [:create [:step 42 0.033 1] [:destroy 42]] @calls))))

(deftest reset-recreates-and-render-plan-is-plain-data
  (let [calls (atom [])
        adapter (physics/init (callbacks calls))
        reset (physics/reset adapter)
        plan (physics/render-plan (siege/new-game))]
    (is (= 42 (:world-id reset)))
    (is (= [:create [:destroy 42] :create] @calls))
    (is (= 75 (count (:castle plan))))
    (is (not (contains? plan :world-id)))))
