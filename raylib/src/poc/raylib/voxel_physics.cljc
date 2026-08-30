(ns poc.raylib.voxel-physics
  "Owner-affine Box3D lifecycle boundary for Voxel Siege.

  Callbacks are injected by the platform adapter and must only be invoked by
  the Raylib/Jolt frame owner. Native IDs never enter the pure game state."
  (:require [poc.raylib.voxel-siege :as siege]))

(def max-delta-seconds siege/max-delta-seconds)

(defn ^:export init
  "Create one native world and retain its opaque ID only in adapter state."
  [callbacks]
  (when-not (every? (comp fn? callbacks) [:world-create :world-step :world-destroy])
    (throw (ex-info "Incomplete Voxel physics callbacks" {:callbacks (keys callbacks)})))
  {:status :active
   :callbacks callbacks
   :world-id ((:world-create callbacks) 0.0 -25.0 0.0 1)
   :step-count 0})

(defn ^:export step
  "Advance the native world with a bounded dt and return plain-data facts."
  [adapter dt]
  (if (= :active (:status adapter))
    (let [bounded (max 0.0 (min max-delta-seconds (double dt)))]
      ((:world-step (:callbacks adapter)) (:world-id adapter) bounded 1)
      [(update adapter :step-count inc)
       {:dt bounded :step (:step-count adapter) :world-id? true}])
    [adapter {:dt 0.0 :step (:step-count adapter) :world-id? false}]))

(defn ^:export dispose
  "Destroy at most once; a disposed adapter cannot invoke native callbacks."
  [adapter]
  (if (= :active (:status adapter))
    (do
      ((:world-destroy (:callbacks adapter)) (:world-id adapter))
      (assoc adapter :status :disposed :world-id nil))
    adapter))

(defn ^:export reset
  "Dispose and recreate the world without exposing its ID to scene state."
  [adapter]
  (init (:callbacks (dispose adapter))))

(defn ^:export render-plan
  "Pure render description consumed by an owner-affine Raylib renderer."
  [state]
  {:castle (mapv (fn [[x y z]] {:x x :y y :z z :kind :voxel}) (:cells state))
   :aim (:aim state)
   :shots-left (:balls-left state)
   :destruction (siege/destruction state)})
