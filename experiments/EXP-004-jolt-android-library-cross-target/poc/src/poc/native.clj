(ns poc.native
  (:require [jolt.ffi :as ffi]))

(defn answer [] 42)

(defn allocate [n]
  (loop [i 0 values []]
    (if (= i n)
      (count values)
      (recur (+ i 1) (conj values i)))))

(ffi/export! "poc_answer" answer [] :int)
(ffi/export! "poc_allocate" allocate [:int] :int)
