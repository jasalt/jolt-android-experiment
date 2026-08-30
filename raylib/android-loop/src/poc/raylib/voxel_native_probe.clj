(ns poc.raylib.voxel-native-probe
  "Optional process-symbol Box3D probe; called only by an Android host probe."
  (:require [jolt.ffi :as ffi]))

(ffi/defcfn world-create "vb3_world_create" [:double :double :double :int] :uint)
(ffi/defcfn world-step "vb3_world_step" [:uint :float :int] :void)
(ffi/defcfn world-destroy "vb3_world_destroy" [:uint] :void)

(defn ^:export smoke
  "Create, step and destroy one scalar-ABI world on the current owner thread."
  []
  (let [world (world-create 0.0 -9.8 0.0 1)]
    (dotimes [_ 10] (world-step world 0.016 1))
    (world-destroy world)
    {:world world :steps 10 :status :ok}))

(defn ffi-probe []
  (smoke)
  1)

(ffi/export! "raylib_voxel_native_probe" ffi-probe [] :int)
