(ns poc.raylib.abi-runner
  (:require [jolt.ffi :as ffi]
            [poc.raylib.abi :as abi]))

(defn -main [& _]
  (let [library (System/getenv "RAYLIB_ABI_ORACLE")]
    (when (or (nil? library) (= "" library))
      (throw (ex-info "RAYLIB_ABI_ORACLE is required" {})))
    (ffi/load-library library)
    (println (pr-str (abi/verify!)))))
