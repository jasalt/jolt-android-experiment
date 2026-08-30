(ns poc.raylib.loop
  "Jolt-owned persistent Raylib loop with touch-first adaptive diagnostics."
  (:require [jolt.ffi :as ffi]
            [poc.raylib.diagnostics :as diagnostics]))

(declare init-window set-target-fps should-close-raw begin-drawing
         clear-background draw-text draw-circle end-drawing get-frame-time
         get-screen-width get-screen-height get-render-width get-render-height
         get-touch-point-count get-touch-point-id get-touch-x get-touch-y
         get-mouse-x get-mouse-y mouse-pressed-raw mouse-down-raw
         mouse-released-raw is-key-pressed-raw android-log-write close-window)
(ffi/defcfn init-window "InitWindow" [:int :int :string] :void)
(ffi/defcfn set-target-fps "SetTargetFPS" [:int] :void)
(ffi/defcfn ^:private should-close-raw "WindowShouldClose" [] :int)
(ffi/defcfn begin-drawing "BeginDrawing" [] :void)
(ffi/defcfn clear-background "ClearBackground" [:uint] :void)
(ffi/defcfn draw-text "DrawText" [:string :int :int :int :uint] :void)
(ffi/defcfn draw-circle "DrawCircle" [:int :int :float :uint] :void)
(ffi/defcfn end-drawing "EndDrawing" [] :void)
(ffi/defcfn get-frame-time "GetFrameTime" [] :float)
(ffi/defcfn get-screen-width "GetScreenWidth" [] :int)
(ffi/defcfn get-screen-height "GetScreenHeight" [] :int)
(ffi/defcfn get-render-width "GetRenderWidth" [] :int)
(ffi/defcfn get-render-height "GetRenderHeight" [] :int)
(ffi/defcfn get-touch-point-count "GetTouchPointCount" [] :int)
(ffi/defcfn get-touch-point-id "GetTouchPointId" [:int] :int)
(ffi/defcfn get-touch-x "GetTouchX" [] :int)
(ffi/defcfn get-touch-y "GetTouchY" [] :int)
(ffi/defcfn get-mouse-x "GetMouseX" [] :int)
(ffi/defcfn get-mouse-y "GetMouseY" [] :int)
(ffi/defcfn ^:private mouse-pressed-raw "IsMouseButtonPressed" [:int] :int)
(ffi/defcfn ^:private mouse-down-raw "IsMouseButtonDown" [:int] :int)
(ffi/defcfn ^:private mouse-released-raw "IsMouseButtonReleased" [:int] :int)
(ffi/defcfn ^:private is-key-pressed-raw "IsKeyPressed" [:int] :int)
(ffi/defcfn android-log-write "__android_log_write" [:int :string :string] :int)
(ffi/defcfn close-window "CloseWindow" [] :void)

(def MOUSE-BUTTON-LEFT 0)
(def KEY-BACK 4)
(def ANDROID-LOG-INFO 4)

(defn rgba [r g b a]
  (bit-or (int r) (bit-shift-left (int g) 8)
          (bit-shift-left (int b) 16) (bit-shift-left (int a) 24)))

(def RAYWHITE (rgba 245 245 245 255))
(def DARKBLUE (rgba 0 82 172 255))
(def DARKGRAY (rgba 80 80 80 255))
(def MAROON (rgba 190 33 55 255))
(def SKYBLUE (rgba 102 191 255 255))

(defn true-raw? [value]
  (not (zero? (bit-and value 0xff))))

(defn poll-input []
  (let [touch-count (max 0 (get-touch-point-count))
        touch-ids (mapv get-touch-point-id (range touch-count))
        touch? (pos? touch-count)]
    (diagnostics/normalize-input
     {:screen-width (get-screen-width)
      :screen-height (get-screen-height)
      :render-width (get-render-width)
      :render-height (get-render-height)
      :touch-count touch-count
      :touch-ids touch-ids
      :pointer-x (if touch? (get-touch-x) (get-mouse-x))
      :pointer-y (if touch? (get-touch-y) (get-mouse-y))
      :pressed? (true-raw? (mouse-pressed-raw MOUSE-BUTTON-LEFT))
      :down? (or touch? (true-raw? (mouse-down-raw MOUSE-BUTTON-LEFT)))
      :released? (true-raw? (mouse-released-raw MOUSE-BUTTON-LEFT))
      :back? (or (true-raw? (is-key-pressed-raw KEY-BACK))
                 (true-raw? (should-close-raw)))})))

(defn log-state! [frame input state]
  (android-log-write
   ANDROID-LOG-INFO "jolt_raylib_touch_state"
   (pr-str {:frame frame
            :metrics (:metrics input)
            :pointer (:pointer input)
            :touches (:touches input)
            :tap-count (:tap-count state)
            :hold-frames (:hold-frames state)
            :drag-samples (:drag-samples state)
            :close-requested? (:close-requested? state)})))

(defn draw-diagnostics! [frame frame-us input state]
  (let [metrics (:metrics input)
        {:keys [margin title-size body-size line-gap touch-radius]}
        (diagnostics/layout metrics)
        [screen-width screen-height] (:screen metrics)
        [render-width render-height] (:render metrics)
        [scale-x scale-y] (:dpi-scale metrics)
        phase (get-in input [:pointer :phase])
        point (get-in input [:pointer :position])
        touches (:touches input)]
    (begin-drawing)
    (clear-background RAYWHITE)
    (draw-text "Jolt + Raylib touch diagnostics" margin margin title-size DARKBLUE)
    (draw-text (str "Screen " screen-width "x" screen-height
                    " | render " render-width "x" render-height)
               margin (+ margin title-size line-gap) body-size DARKGRAY)
    (draw-text (str "Orientation " (name (:orientation metrics))
                    " | DPI scale " scale-x "x" scale-y)
               margin (+ margin title-size (* 2 line-gap)) body-size DARKGRAY)
    (draw-text (str "Touches " (:count touches) " | IDs " (:ids touches)
                    " | point-0 " (:point-0 touches))
               margin (+ margin title-size (* 3 line-gap)) body-size MAROON)
    (draw-text (str "Pointer " (name phase) " | taps " (:tap-count state)
                    " | hold frames " (:hold-frames state)
                    " | drag samples " (:drag-samples state))
               margin (+ margin title-size (* 4 line-gap)) body-size MAROON)
    (draw-text (str "Frame " frame " | frame time us " frame-us)
               margin (+ margin title-size (* 5 line-gap)) body-size DARKGRAY)
    (draw-text "Point-0 coordinates only | all-point Vector2 ABI unproven"
               margin (+ margin title-size (* 6 line-gap)) body-size DARKGRAY)
    (draw-text "Android Back closes | Linux mouse fallback"
               margin (+ margin title-size (* 7 line-gap)) body-size DARKGRAY)
    (when point
      (draw-circle (first point) (second point) (float touch-radius) SKYBLUE))
    (end-drawing)))

(defn run-loop []
  (let [duration-ms 905000
        target-fps 30
        start (System/currentTimeMillis)]
    (init-window 0 0 "Jolt Raylib touch diagnostics")
    (set-target-fps target-fps)
    (try
      (loop [frame 0 state diagnostics/initial-state]
        (let [elapsed (- (System/currentTimeMillis) start)
              frame-us (int (* (get-frame-time) 1000000))
              input (poll-input)
              next-state (diagnostics/step state input)
              phase (get-in input [:pointer :phase])]
          (when (or (zero? (mod frame 150))
                    (not= :idle phase)
                    (:back? input))
            (log-state! frame input next-state))
          (if (or (>= elapsed duration-ms) (:close-requested? next-state))
            frame
            (do
              (draw-diagnostics! frame frame-us input next-state)
              (recur (inc frame) next-state)))))
      (finally (close-window)))))

(ffi/export! "raylib_persistent_loop" run-loop [] :int)
