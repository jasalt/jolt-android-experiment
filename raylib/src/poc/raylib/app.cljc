(ns poc.raylib.app
  "Pure Raylib-host adapter for the shared reducer.

  Raylib pointer and keyboard snapshots are translated into the existing
  portable event vocabulary here. No Raylib structs or native values cross
  into the reducer."
  (:require [poc.reducer :as reducer]
            [poc.raylib.gallery-ui :as gallery-ui]))

(defn ^:export counter-event
  "Translate a scene-local control press or keyboard fallback into a shared
  reducer event. Navigation controls are intentionally handled by gallery."
  [mode input controls]
  (when (= :scene mode)
    (let [phase (get-in input [:pointer :phase])
          point (get-in input [:pointer :position])
          action (when (and (= :press phase) point)
                   (some (fn [control]
                           (when (gallery-ui/contains-point? control point)
                             (:action control)))
                         controls))]
      (cond
        (= :increment action) {:type :counter/inc}
        (= :decrement action) {:type :counter/dec}
        (= :reset action) {:type :counter/reset}
        (get-in input [:keyboard :activate?]) {:type :counter/inc}
        (get-in input [:keyboard :previous?]) {:type :counter/dec}))))

(defn ^:export step
  "Apply one translated event through the unchanged portable reducer."
  [state event]
  (if event
    (reducer/step state event)
    [state []]))
