(ns poc.native
  (:require [jolt.ffi :as ffi]))

(defn answer [] 42)

(ffi/export! "poc_answer" answer [] :int)
