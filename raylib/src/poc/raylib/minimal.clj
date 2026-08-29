(ns poc.raylib.minimal
  "Smallest Raylib FFI surface for the Android first-frame path.

  This namespace deliberately has no :jolt/native declaration. The desktop
  runner loads the dynamic Nix library explicitly; a future Android topology
  provides either process-symbol lookup or its own native-library declaration.
  Keeping that choice outside this binding is what permits the upstream source
  shape to be reused without copying its example suite."
  (:require [jolt.ffi :as ffi]))

(defn load-desktop-library!
  "Load the Raylib shared library for desktop run/build-image smoke tests.

  RAYLIB_LIBRARY_PATH is supplied by the pinned Nix shell. Android must not use
  this function: its final native topology is an explicit later experiment."
  []
  (let [path (System/getenv "RAYLIB_LIBRARY_PATH")]
    (when (or (nil? path) (= "" path))
      (throw (ex-info "RAYLIB_LIBRARY_PATH is required for the desktop Raylib smoke"
                      {})))
    (ffi/load-library (str path "/libraylib.so.6"))))

;; First-frame scalar ABI only. Color is four u8 fields, passed as one :uint on
;; both tested desktop ABIs; larger by-value structs belong to the later ABI task.
;; The declarations keep editor analysis aware of vars created by the FFI macro;
;; `defcfn` remains the runtime definition.
(declare init-window should-close-raw begin-drawing clear-background draw-text
         end-drawing close-window get-screen-width get-screen-height
         set-target-fps)
(ffi/defcfn init-window "InitWindow" [:int :int :string] :void)
(ffi/defcfn ^:private should-close-raw "WindowShouldClose" [] :int)
(ffi/defcfn begin-drawing "BeginDrawing" [] :void)
(ffi/defcfn clear-background "ClearBackground" [:uint] :void)
(ffi/defcfn draw-text "DrawText" [:string :int :int :int :uint] :void)
(ffi/defcfn end-drawing "EndDrawing" [] :void)
(ffi/defcfn close-window "CloseWindow" [] :void)
(ffi/defcfn get-screen-width "GetScreenWidth" [] :int)
(ffi/defcfn get-screen-height "GetScreenHeight" [] :int)
(ffi/defcfn ^:private set-target-fps "SetTargetFPS" [:int] :void)

(defn rgba [r g b a]
  (bit-or (int r) (bit-shift-left (int g) 8)
          (bit-shift-left (int b) 16) (bit-shift-left (int a) 24)))

(def RAYWHITE (rgba 245 245 245 255))
(def DARKBLUE (rgba 0 82 172 255))
(def MAROON (rgba 190 33 55 255))

(defn window-should-close? []
  ;; Raylib booleans are C int values; use the defined low-byte truth value.
  (not (zero? (bit-and (should-close-raw) 0xff))))

(defn desktop-smoke! []
  (load-desktop-library!)
  (init-window 640 360 "Jolt Raylib minimal binding smoke")
  (set-target-fps 60)
  (try
    (dotimes [frame 45]
      (when-not (window-should-close?)
        (begin-drawing)
        (clear-background RAYWHITE)
        (draw-text "Jolt + Raylib minimal bindings" 24 48 24 DARKBLUE)
        (draw-text (str "Screen: " (get-screen-width) " x " (get-screen-height))
                   24 90 20 MAROON)
        (draw-text (str "Frame: " frame) 24 128 20 MAROON)
        (end-drawing)))
    (finally (close-window))))

(defn -main [& _]
  (desktop-smoke!))
