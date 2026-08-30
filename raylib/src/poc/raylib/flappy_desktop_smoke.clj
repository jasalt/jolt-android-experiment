(ns poc.raylib.flappy-desktop-smoke
  "Linux desktop keyboard smoke for the shared pure Flappy Bird simulation.

  This is deliberately a thin desktop adapter, not a second gallery loop. It
  loads the Nix-provided Raylib library, sends Space press edges to the same
  scene state machine used by Android, renders for a bounded frame count, and
  exits normally."
  (:require [jolt.ffi :as ffi]
            [poc.raylib.flappy-bird :as flappy]))

(declare init-window set-target-fps begin-drawing clear-background draw-text
         draw-rectangle draw-circle end-drawing close-window get-screen-width
         get-screen-height get-frame-time key-pressed-raw window-ready-raw)
(ffi/defcfn init-window "InitWindow" [:int :int :string] :void)
(ffi/defcfn set-target-fps "SetTargetFPS" [:int] :void)
(ffi/defcfn begin-drawing "BeginDrawing" [] :void)
(ffi/defcfn clear-background "ClearBackground" [:uint] :void)
(ffi/defcfn draw-text "DrawText" [:string :int :int :int :uint] :void)
(ffi/defcfn draw-rectangle "DrawRectangle" [:int :int :int :int :uint] :void)
(ffi/defcfn draw-circle "DrawCircle" [:int :int :float :uint] :void)
(ffi/defcfn end-drawing "EndDrawing" [] :void)
(ffi/defcfn close-window "CloseWindow" [] :void)
(ffi/defcfn get-screen-width "GetScreenWidth" [] :int)
(ffi/defcfn get-screen-height "GetScreenHeight" [] :int)
(ffi/defcfn get-frame-time "GetFrameTime" [] :float)
(ffi/defcfn ^:private key-pressed-raw "IsKeyPressed" [:int] :int)
(ffi/defcfn ^:private window-ready-raw "IsWindowReady" [] :int)

(def KEY-SPACE 32)

(defn rgba [r g b a]
  (bit-or (int r) (bit-shift-left (int g) 8)
          (bit-shift-left (int b) 16) (bit-shift-left (int a) 24)))

(def SKYBLUE (rgba 102 191 255 255))
(def DARKGREEN (rgba 0 117 44 255))
(def GOLD (rgba 255 203 0 255))
(def DARKGRAY (rgba 80 80 80 255))
(def MAROON (rgba 190 33 55 255))

(defn load-desktop-library! []
  (let [path (System/getenv "RAYLIB_LIBRARY_PATH")]
    (when (or (nil? path) (= "" path))
      (throw (ex-info "RAYLIB_LIBRARY_PATH is required" {})))
    (ffi/load-library (str path "/libraylib.so.6"))))

(defn- window-ready? []
  (not (zero? (bit-and (window-ready-raw) 0xff))))

(defn- pressed? []
  (not (zero? (bit-and (key-pressed-raw KEY-SPACE) 0xff))))

(defn- metrics []
  {:screen [(get-screen-width) (get-screen-height)]})

(defn- draw! [frame state metrics]
  (let [{:keys [height bird-x bird-radius pipe-width gap-height]}
        (flappy/dimensions metrics)]
    (begin-drawing)
    (clear-background SKYBLUE)
    (draw-text "Flappy Bird desktop smoke" 20 18 26 DARKGRAY)
    (draw-text "Space flaps; bounded smoke exits automatically" 20 52 18 DARKGRAY)
    (doseq [{:keys [x gap]} (:pipes state)]
      (draw-rectangle (int x) 0 (int pipe-width) (int gap) DARKGREEN)
      (draw-rectangle (int x) (int (+ gap gap-height)) (int pipe-width)
                      (int (- height (+ gap gap-height))) DARKGREEN))
    (draw-circle (int bird-x) (int (:y state)) (double bird-radius) GOLD)
    (draw-text (str "score " (:score state) " | frame " frame) 20 82 20 DARKGRAY)
    (when (:over? state)
      (draw-text "GAME OVER - SPACE TO RESTART" 20 (int (/ height 2.0)) 22 MAROON))
    (end-drawing)))

(defn -main [& _]
  (load-desktop-library!)
  (init-window 800 450 "Jolt Raylib Flappy Bird smoke")
  (when-not (window-ready?)
    (close-window)
    (throw (ex-info "Raylib desktop window initialization failed" {:display (System/getenv "DISPLAY")})))
  (set-target-fps 60)
  (try
    (loop [frame 0
           state (flappy/new-game (metrics) 1337)]
      (when (< frame 180)
        (let [input {:metrics (metrics)
                     :delta-seconds (get-frame-time)
                     :pointer {:phase :idle}
                     :keyboard {:activate? (pressed?)}}
              next-state (flappy/step state input)]
          (draw! frame next-state (:metrics input))
          (recur (inc frame) next-state))))
    (finally (close-window))))
