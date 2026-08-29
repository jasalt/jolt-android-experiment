(ns poc.raylib.host
  "No-op export used to validate the Raylib host's isolated Jolt library.

  This fixture intentionally does not require jolt.ffi bindings or Raylib. It
  proves only cross-target library construction and ABI-table lookup; the
  NativeActivity bootstrap and Raylib symbol topology are later experiments."
  (:require [jolt.ffi :as ffi]))

(defn raylib-host-noop []
  7)

(ffi/export! "raylib_host_noop" raylib-host-noop [] :int)
