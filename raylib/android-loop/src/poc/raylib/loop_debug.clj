(ns poc.raylib.loop-debug
  "Debug-only Android nREPL bootstrap around the Raylib/Jolt owner loop."
  (:require [jolt.ffi :as ffi]
            [jolt.nrepl :as nrepl]
            [poc.raylib.loop :as loop]))

(def nrepl-port 7888)

(defn run-debug-loop
  "Start loopback nREPL, run the owner loop, then stop nREPL before shutdown.

  nREPL workers may evaluate pure definitions and inspect runtime state. They
  must not invoke Raylib FFI; redefined bodies are called later by the owner, or
  short owner-affine work is submitted through loop/submit-owner!."
  []
  (let [stop-nrepl (nrepl/start nrepl-port)]
    (loop/android-log-write loop/ANDROID-LOG-INFO "jolt_raylib_nrepl"
                            (str "started loopback port=" nrepl-port
                                 " owner-thread=" (.getId (Thread/currentThread))))
    (try
      (loop/run-loop)
      (finally
        (stop-nrepl)
        (loop/android-log-write loop/ANDROID-LOG-INFO "jolt_raylib_nrepl"
                                "stopped before Jolt shutdown")))))

(ffi/export! "raylib_gallery_debug" run-debug-loop [] :int)
