(ns poc.raylib.loop
  "Jolt-owned persistent Raylib loop for the post-first-frame R4 gate.

  The loop is deliberately bounded at fifteen minutes for unattended evidence,
  but it is otherwise a normal update/render loop: C enters Jolt once and Jolt
  owns every Raylib call until orderly shutdown."
  (:require [jolt.ffi :as ffi]))

(declare init-window set-target-fps should-close-raw begin-drawing
         clear-background draw-text end-drawing get-frame-time close-window)
(ffi/defcfn init-window "InitWindow" [:int :int :string] :void)
(ffi/defcfn set-target-fps "SetTargetFPS" [:int] :void)
(ffi/defcfn ^:private should-close-raw "WindowShouldClose" [] :int)
(ffi/defcfn begin-drawing "BeginDrawing" [] :void)
(ffi/defcfn clear-background "ClearBackground" [:uint] :void)
(ffi/defcfn draw-text "DrawText" [:string :int :int :int :uint] :void)
(ffi/defcfn end-drawing "EndDrawing" [] :void)
(ffi/defcfn get-frame-time "GetFrameTime" [] :float)
(ffi/defcfn close-window "CloseWindow" [] :void)

(defn rgba [r g b a]
  (bit-or (int r) (bit-shift-left (int g) 8)
          (bit-shift-left (int b) 16) (bit-shift-left (int a) 24)))

(def RAYWHITE (rgba 245 245 245 255))
(def DARKBLUE (rgba 0 82 172 255))
(def DARKGRAY (rgba 80 80 80 255))
(def MAROON (rgba 190 33 55 255))

(defn window-should-close? []
  (not (zero? (bit-and (should-close-raw) 0xff))))

(defn run-loop []
  (let [duration-ms 905000
        target-fps 30
        start (System/currentTimeMillis)]
    (init-window 0 0 "Jolt Raylib persistent loop")
    (set-target-fps target-fps)
    (try
      (loop [frame 0 min-us 1000000000 max-us 0 total-us 0]
        (let [elapsed (- (System/currentTimeMillis) start)
              frame-time (get-frame-time)
              frame-us (int (* frame-time 1000000))]
          (if (or (>= elapsed duration-ms) (window-should-close?))
            frame
            (do
              (begin-drawing)
              (clear-background RAYWHITE)
              (draw-text "Jolt + Raylib persistent loop" 24 48 30 DARKBLUE)
              (draw-text "All update/render calls stay on the Jolt thread" 24 92 20 DARKGRAY)
              (draw-text (str "Frame: " frame " | elapsed ms: " elapsed) 24 132 20 MAROON)
              (draw-text (str "Frame time us: " frame-us " | target FPS: " target-fps) 24 168 18 DARKGRAY)
              (draw-text "15-minute bounded R4 stability run | API 35 | arm64-v8a" 24 202 16 DARKGRAY)
              (end-drawing)
              (recur (inc frame)
                     (min min-us frame-us)
                     (max max-us frame-us)
                     (+ total-us frame-us))))))
      (finally (close-window)))))

(ffi/export! "raylib_persistent_loop" run-loop [] :int)
