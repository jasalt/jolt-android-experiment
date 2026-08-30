(ns poc.raylib.voxel-siege-test
  (:require [clojure.test :refer [deftest is]]
            [poc.raylib.voxel-siege :as siege]))

(deftest deterministic-round-fixture
  (let [state (siege/new-game)]
    (is (= 5 (:balls-left state)))
    (is (= 75 (:initial-cells state)))
    (is (= [0.0 0.3894183423086505 -0.9210609940028851]
           (siege/direction 0.0 0.4)))
    (is (= 0.15 (siege/power-from-charge 0.0)))
    (is (= 0.9 (siege/power-from-charge 2.0)))))

(deftest charge-release-consumes-one-shot
  (let [state (-> (siege/new-game)
                  siege/press-fire
                  (siege/tick 0.5)
                  siege/release-fire)]
    (is (= 4 (:balls-left state)))
    (is (false? (:charging? state)))
    (is (= 1 (count (:shots state))))
    (is (= 0.4 (get-in state [:shots 0 :aim 1])))))

(deftest mobile-control-precedence
  (let [metrics {:width 960 :height 540}]
    (is (= :reset (:command (siege/input-command metrics nil
                                                 {:phase :press :position [920 20]}))))
    (is (= :toggle-orientation (:command
                                (siege/input-command metrics nil
                                                      {:phase :press :position [30 500]}))))
    (is (= :press-fire (:command (siege/input-command metrics nil
                                                     {:phase :press :position [850 470]}))))
    (is (= :none (:command (siege/input-command metrics nil
                                               {:phase :press :position [15 15]}))))))

(deftest orientation-calibrates-relative-pose
  (let [state (-> (siege/new-game)
                  (siege/calibrate [1.0 0.2])
                  (siege/orientation-aim [1.1 0.3]))]
    (is (:orientation? state))
    (is (< (Math/abs (- 0.1 (get-in state [:aim :yaw]))) 1e-9))
    (is (< (Math/abs (- 0.5 (get-in state [:aim :pitch]))) 1e-9))))

(deftest command-application
  (let [state (-> (siege/new-game)
                  (siege/apply-command {:command :aim-drag :dx 10 :dy -10})
                  (siege/apply-command {:command :press-fire})
                  (siege/apply-command {:command :release-fire}))]
    (is (= 4 (:balls-left state)))
    (is (= 0.08 (get-in state [:aim :yaw])))
    (is (< (Math/abs (- 0.48 (get-in state [:aim :pitch]))) 1e-9))))

(deftest destruction-threshold
  (let [state (siege/new-game #{[0 0 0] [1 0 0] [2 0 0]})
        won (siege/apply-destruction state #{[0 0 0] [1 0 0] [2 0 0]})]
    (is (= 1.0 (siege/destruction won)))
    (is (= :won (:phase won)))))
