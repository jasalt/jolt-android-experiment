(ns poc.raylib.voxel-siege
  "Pure Voxel Siege rules and mobile input adapter.

  Box3D, Raylib, sensors and native handles stay outside this namespace. The
  scene adapter supplies physics facts and consumes the commands returned by
  `input-command`."
  (:require [clojure.set :as set]))

(def balls-per-round 5)
(def win-threshold 0.7)
(def max-delta-seconds 0.033)
(def yaw-range [(- (/ Math/PI 2)) (/ Math/PI 2)])
(def pitch-range [(- (/ Math/PI 6)) (/ Math/PI 3)])

(defn clamp [value [low high]]
  (max low (min high (double value))))

(defn direction [yaw pitch]
  [(* (Math/cos pitch) (Math/sin yaw))
   (Math/sin pitch)
   (- (* (Math/cos pitch) (Math/cos yaw)))])

(defn power-from-charge [seconds]
  (clamp (+ 0.15 (* 0.75 (clamp (/ (double (or seconds 0.0)) 1.5) [0.0 1.0])))
         [0.15 0.9]))

(defn castle-cells
  "Deterministic three-level castle fixture; a cell is [x y z]."
  []
  (set (for [x (range -3 4) y (range 0 5) z (range -1 2)
             :when (or (zero? y) (zero? z) (zero? x) (= x 3) (= y 4))]
         [x y z])))

(defn new-game
  ([] (new-game (castle-cells)))
  ([cells]
   {:phase :playing
    :balls-left balls-per-round
    :aim {:yaw 0.0 :pitch 0.4}
    :charge-seconds 0.0
    :charging? false
    :baseline-pose nil
    :orientation? false
    :initial-cells (count cells)
    :destroyed-cells #{ }
    :cells (set cells)
    :shots []}))

(defn destruction [state]
  (if (zero? (:initial-cells state))
    0.0
    (/ (double (count (:destroyed-cells state))) (:initial-cells state))))

(defn phase-after-destruction [state]
  (cond
    (>= (destruction state) win-threshold) :won
    (and (zero? (:balls-left state)) (not (:charging? state))) :lost
    :else (:phase state)))

(defn aim-drag [state dx dy]
  (update state :aim (fn [{:keys [yaw pitch]}]
                       {:yaw (clamp (+ yaw (* 0.008 (double dx))) yaw-range)
                        :pitch (clamp (- pitch (* 0.008 (double dy))) pitch-range)})))

(defn calibrate [state pose]
  (assoc state :baseline-pose pose :orientation? true))

(defn orientation-aim [state [yaw pitch]]
  (let [[base-yaw base-pitch] (or (:baseline-pose state) [0.0 0.0])]
    (assoc-in state [:aim]
              {:yaw (clamp (- yaw base-yaw) yaw-range)
               :pitch (clamp (+ 0.4 (- pitch base-pitch)) pitch-range)})))

(defn ^:export transform-sensor
  "Map Android sensor x/y coordinates into the active display rotation."
  [[x y] rotation]
  (case rotation
    90 [(- y) x]
    180 [(- x) (- y)]
    270 [y (- x)]
    [x y]))

(defn ^:export relative-quaternion
  "Return current relative to the calibration quaternion [x y z w]."
  [[bx by bz bw] [cx cy cz cw]]
  (let [[ix iy iz iw] [(- bx) (- by) (- bz) bw]]
    [(+ (* iw cx) (* ix cw) (* iy cz) (- (* iz cy)))
     (+ (* iw cy) (- (* ix cz)) (* iy cw) (* iz cx))
     (+ (* iw cz) (* ix cy) (- (* iy cx)) (* iz cw))
     (- (* iw cw) (* ix cx) (* iy cy) (* iz cz))]))

(defn ^:export quaternion-yaw-pitch
  "Extract bounded yaw/pitch from a relative quaternion."
  [[x y z w]]
  [(Math/atan2 (* 2.0 (+ (* w y) (* x z)))
               (- 1.0 (* 2.0 (+ (* y y) (* z z)))))
   (Math/asin (clamp (* 2.0 (- (* w x) (* y z))) [-1.0 1.0]))])

(defn press-fire [state]
  (if (and (= :playing (:phase state)) (pos? (:balls-left state))
           (not (:charging? state)))
    (assoc state :charging? true :charge-seconds 0.0)
    state))

(defn release-fire [state]
  (if (:charging? state)
    (let [{:keys [yaw pitch]} (:aim state)
          next-state (-> state
                         (assoc :charging? false :charge-seconds 0.0)
                         (update :balls-left dec)
                         (update :shots conj {:aim [yaw pitch]
                                              :direction (direction yaw pitch)
                                              :power (power-from-charge (:charge-seconds state))}))]
      (assoc next-state :phase (phase-after-destruction next-state)))
    state))

(defn tick [state dt]
  (if (:charging? state)
    (update state :charge-seconds + (min max-delta-seconds (max 0.0 (double dt))))
    state))

(defn reset [state]
  (new-game (:initial-cells state)))

(defn- scene-metrics [input]
  (let [[width height] (get-in input [:metrics :screen] [960 540])]
    {:width width :height height}))

(declare input-command apply-command)

(defn ^:export scene
  "Pure scene descriptor consumed by the shared gallery host."
  []
  {:id :voxel-siege
   :title "Voxel Siege"
   :orientation :landscape
   :init (fn [_] [(new-game) [[:scene/init :voxel-siege]]])
   :update (fn [state input]
             (let [pointer (:pointer input)
                   routed (input-command (scene-metrics input) state
                                         {:phase (:phase pointer)
                                          :position (:position pointer)})
                   command (case (:command routed)
                             :reset {:command :reset}
                             :press-fire {:command :press-fire}
                             :release-fire {:command :release-fire}
                             :toggle-orientation {:command :toggle-orientation :pose [0 0]}
                             :aim-drag {:command :aim-drag :dx 0 :dy 0}
                             nil)]
               [(if command (apply-command state command) state) []]))
   :draw (fn [state _] [state []])
   :dispose (fn [state] [state [[:scene/dispose :voxel-siege]]])})

(defn apply-destruction [state cells]
  (let [destroyed (set/intersection (:cells state) (set cells))
        next-state (-> state
                       (update :destroyed-cells into destroyed)
                       (update :cells #(set/difference % destroyed)))]
    (assoc next-state :phase (phase-after-destruction next-state))))

(defn control-rects [{:keys [width height]}]
  {:back [12 12 64 48]
   :reset [(- width 64) 12 48 40]
   :mode [12 (- height 64) 116 48]
   :fire [(- width 164) (- height 112) 148 96]})

(defn inside? [[x y w h] [px py]]
  (and (<= x px (+ x w)) (<= y py (+ y h))))

(defn input-command
  "Resolve one normalized pointer event with deterministic control precedence."
  [metrics state {:keys [phase position]}]
  (let [rects (control-rects metrics)]
    (cond
      (= phase :back) {:command :back}
      (and (= phase :press) (inside? (:reset rects) position)) {:command :reset}
      (and (= phase :press) (inside? (:mode rects) position)) {:command :toggle-orientation}
      (inside? (:fire rects) position) {:command (if (= phase :release) :release-fire :press-fire)}
      (and (:orientation? state) (= phase :press)) {:command :press-fire}
      (and (:orientation? state) (= phase :release)) {:command :release-fire}
      :else {:command (if (= phase :drag) :aim-drag :none)
             :position position})))

(defn apply-command
  "Apply a normalized command; platform adapters do hit testing only once and
  pass drag deltas or orientation samples as command data."
  [state {:keys [command dx dy pose]}]
  (case command
    :reset (reset state)
    :press-fire (press-fire state)
    :release-fire (release-fire state)
    :aim-drag (aim-drag state dx dy)
    :calibrate (calibrate state pose)
    :orientation-aim (orientation-aim state pose)
    :toggle-orientation (if (:orientation? state)
                          (assoc state :orientation? false :baseline-pose nil)
                          (calibrate state pose))
    state))
