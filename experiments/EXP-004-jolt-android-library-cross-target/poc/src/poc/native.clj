(ns poc.native
  (:require [clojure.edn :as edn]
            [jolt.ffi :as ffi]
            [poc.reducer :as reducer]))

(defn answer [] 42)

(defn allocate [n]
  (loop [i 0 values []]
    (if (= i n) (count values) (recur (+ i 1) (conj values i)))))

(defn dispatch-counter [event-edn]
  (let [[model _] (reducer/step reducer/initial-state (edn/read-string event-edn))]
    (:counter model)))

(ffi/export! "poc_answer" answer [] :int)
(ffi/export! "poc_allocate" allocate [:int] :int)
(ffi/export! "poc_dispatch_counter" dispatch-counter [:string] :int)
