(ns poc.raylib.topology
  "Small process-symbol topology probe.

  GetScreenWidth is a read-only Raylib accessor. Before a window is created it
  returns Raylib's zero-initialized screen width, so this probe does not start
  EGL or draw a frame."
  (:require [jolt.ffi :as ffi]))

(declare get-screen-width)
(ffi/defcfn get-screen-width "GetScreenWidth" [] :int)

(defn process-screen-width []
  (get-screen-width))

(ffi/export! "raylib_process_screen_width" process-screen-width [] :int)
