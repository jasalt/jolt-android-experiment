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

(ffi/export! "poc_answer" answer [] :int)
(ffi/export! "poc_allocate" allocate [:int] :int)
(ffi/export! "poc_dispatch_counter" dispatch-counter [:string] :int)
