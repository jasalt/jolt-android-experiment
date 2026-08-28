(ns poc.native
  (:require [clojure.edn :as edn]
            [jolt.ffi :as ffi]
            [poc.reducer :as reducer]))

(def app-state (atom reducer/initial-state))

(defn answer [] 42)

(defn allocate [n]
  (loop [i 0 values []]
    (if (= i n) (count values) (recur (+ i 1) (conj values i)))))

(defn dispatch-counter [event-edn]
  (let [event (edn/read-string event-edn)
        [model _] (reducer/step @app-state event)]
    (reset! app-state model)
    (:counter model)))

(defn lifecycle-code []
  (case (:lifecycle @app-state)
    :created 1
    :started 2
    :resumed 3
    0))

(defn effect-code [event-edn]
  (let [event (edn/read-string event-edn)
        [_ effects] (reducer/step @app-state event)]
    (if (= :platform/clipboard (:type (first effects))) 1 0)))

(ffi/export! "poc_answer" answer [] :int)
(ffi/export! "poc_allocate" allocate [:int] :int)
(ffi/export! "poc_dispatch_counter" dispatch-counter [:string] :int)
(ffi/export! "poc_lifecycle_code" lifecycle-code [] :int)
(ffi/export! "poc_effect_code" effect-code [:string] :int)
