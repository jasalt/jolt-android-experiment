(ns poc.raylib.loop
  "Jolt-owned persistent Raylib loop with touch-first adaptive diagnostics."
  (:require [jolt.ffi :as ffi]
            [poc.reducer :as reducer]
            [poc.raylib.abi]
            [poc.raylib.diagnostics :as diagnostics]
            [poc.raylib.flappy-bird :as flappy]
            [poc.raylib.following-eyes :as eyes]
            [poc.raylib.touch-trail :as trail]
            [poc.raylib.touch-diagnostics :as touch-diagnostics]
            [poc.raylib.gesture-diagnostics :as gestures]
            [poc.raylib.app :as app]
            [poc.raylib.gallery :as gallery]
            [poc.raylib.gallery-ui :as gallery-ui]
            [poc.raylib.repl-queue :as repl-queue]))

(declare init-window set-target-fps should-close-raw begin-drawing
         clear-background draw-text draw-circle end-drawing get-frame-time
         get-screen-width get-screen-height get-render-width get-render-height
         get-touch-point-count get-touch-point-id get-touch-x get-touch-y
         get-gesture-detected get-mouse-x get-mouse-y mouse-pressed-raw
         mouse-down-raw mouse-released-raw is-key-pressed-raw
         android-log-write draw-rectangle draw-rectangle-lines close-window)
(ffi/defcfn init-window "InitWindow" [:int :int :string] :void)
(ffi/defcfn set-target-fps "SetTargetFPS" [:int] :void)
(ffi/defcfn ^:private should-close-raw "WindowShouldClose" [] :int)
(ffi/defcfn begin-drawing "BeginDrawing" [] :void)
(ffi/defcfn clear-background "ClearBackground" [:uint] :void)
(ffi/defcfn draw-text "DrawText" [:string :int :int :int :uint] :void)
(ffi/defcfn draw-circle "DrawCircle" [:int :int :float :uint] :void)
(ffi/defcfn draw-rectangle "DrawRectangle" [:int :int :int :int :uint] :void)
(ffi/defcfn draw-rectangle-lines "DrawRectangleLines" [:int :int :int :int :uint] :void)
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
(ffi/defcfn get-gesture-detected "GetGestureDetected" [] :int)
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
(def KEY-ESCAPE 256)
(def KEY-ENTER 257)
(def KEY-RIGHT 262)
(def KEY-LEFT 263)
(def ANDROID-LOG-INFO 4)

(defn rgba [r g b a]
  (bit-or (int r) (bit-shift-left (int g) 8)
          (bit-shift-left (int b) 16) (bit-shift-left (int a) 24)))

(def RAYWHITE (rgba 245 245 245 255))
(def DARKGRAY (rgba 80 80 80 255))
(def MAROON (rgba 190 33 55 255))
(def CARD-BLUE (rgba 35 92 150 255))
(def CARD-DARK (rgba 24 50 78 255))
(def SKYBLUE (rgba 102 191 255 255))
(def DARKGREEN (rgba 0 117 44 255))
(def GOLD (rgba 255 203 0 255))
(def LIGHTGRAY (rgba 200 200 200 255))

(defn placeholder-scene [scene-id title]
  {:id scene-id
   :title title
   :init (fn [input]
           [{:frame 0
             :last-phase (get-in input [:pointer :phase])
             :last-position (get-in input [:pointer :position])}
            [[:scene/init scene-id]]])
   :update (fn [state input]
             [(assoc state
                     :frame (inc (:frame state))
                     :last-phase (get-in input [:pointer :phase])
                     :last-position (get-in input [:pointer :position]))
              []])
   :draw (fn [state _] [state []])
   :dispose (fn [state] [state [[:scene/dispose scene-id]]])})

;; Keep the registration explicit and deterministic. These placeholders are
;; the shell's navigable targets; each leaf scene can later replace only its
;; descriptor without changing navigation or native window ownership.
(def core-scenes
  [(eyes/scene)
   (trail/scene)
   (flappy/scene)
   (placeholder-scene :virtual-controls "Virtual Controls")
   (touch-diagnostics/scene)
   (gestures/scene)])
(def scene-registry (gallery/make-registry core-scenes))
(def scene-ids (mapv :id core-scenes))
(def runtime-state
  (atom {:status :not-started
         :frame 0
         :presentation :baseline}))
(def owner-work (atom repl-queue/initial-state))
(def owner-request-sequence (atom 0))

(defn submit-owner!
  "Queue bounded no-argument work for the Raylib frame owner.

  The nREPL worker only stores the closure. One request is executed between
  frames; query its eventual data result with owner-result."
  [work]
  (when-not (fn? work)
    (throw (ex-info "owner work must be a no-argument function" {})))
  (let [request-id (swap! owner-request-sequence inc)
        request {:id request-id :work work}]
    (loop []
      (let [before @owner-work
            [after accepted?] (repl-queue/enqueue before request)]
        (cond
          (not accepted?) {:status :queue-full :id request-id}
          (compare-and-set! owner-work before after)
          {:status :queued :id request-id}
          :else (recur))))))

(defn owner-result
  "Return a completed owner request result, or nil while queued/unknown."
  [request-id]
  (repl-queue/result @owner-work request-id))

(def debug-repl-functions
  {:submit-owner! submit-owner!
   :owner-result owner-result})

(defn current-runtime-state
  "Return a pure snapshot suitable for nREPL inspection."
  []
  (assoc @runtime-state
         :owner-queue {:pending (count (:pending @owner-work))
                       :operations (vec (keys debug-repl-functions))}))

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
      :gesture-code (get-gesture-detected)
      :keyboard-activate? (true-raw? (is-key-pressed-raw KEY-ENTER))
      :keyboard-previous? (true-raw? (is-key-pressed-raw KEY-LEFT))
      :keyboard-next? (true-raw? (is-key-pressed-raw KEY-RIGHT))
      :keyboard-back? (true-raw? (is-key-pressed-raw KEY-ESCAPE))
      :back? (or (true-raw? (is-key-pressed-raw KEY-BACK))
                 (true-raw? (should-close-raw)))})))

(defn log-state! [frame input diagnostic-state gallery-state app-state effects event]
  (android-log-write
   ANDROID-LOG-INFO "jolt_raylib_gallery_state"
   (pr-str {:frame frame
            :metrics (:metrics input)
            :pointer (:pointer input)
            :touches (:touches input)
            :gesture (:gesture input)
            :keyboard (:keyboard input)
            :gallery-contract gallery/contract-version
            :gallery-mode (:mode gallery-state)
            :selected-scene (:active-scene-id gallery-state)
            :scene-frame (get-in gallery-state [:scene-state :frame])
            :shared-model (reducer/view-model app-state)
            :effects effects
            :last-event event
            :tap-count (:tap-count diagnostic-state)
            :hold-frames (:hold-frames diagnostic-state)
            :drag-samples (:drag-samples diagnostic-state)
            :close-requested? (:close-requested? gallery-state)
            :runtime-state (current-runtime-state)})))

(defn- gallery-layout [input]
  (gallery-ui/gallery-layout (:metrics input) scene-ids
                             (diagnostics/layout (:metrics input))))

(defn- scene-controls [input]
  (gallery-ui/counter-controls (:metrics input)
                               (diagnostics/layout (:metrics input))))

(defn- presentation-color [presentation key fallback]
  (apply rgba (get presentation key fallback)))

(defn- draw-rectangle! [rect fill-color border-color]
  (draw-rectangle (:x rect) (:y rect) (:width rect) (:height rect) fill-color)
  (draw-rectangle-lines (:x rect) (:y rect) (:width rect) (:height rect)
                        border-color))

(defn- effects-label [effects]
  (if-let [effect (first effects)]
    (name (:type effect))
    "none"))

(defn draw-gallery! [frame input presentation]
  (let [metrics (:metrics input)
        sizes (diagnostics/layout metrics)
        {:keys [margin title-size body-size line-gap]} sizes
        layout (gallery-layout input)
        background (presentation-color presentation :background [245 245 245 255])
        accent (presentation-color presentation :accent [0 82 172 255])
        card-color (presentation-color presentation :card [35 92 150 255])
        [width height] (:screen metrics)]
    (begin-drawing)
    (clear-background background)
    (draw-text (:title presentation) margin margin title-size accent)
    (draw-text (str (:subtitle presentation) " | " width "x" height
                    " | " (name (:orientation metrics)))
               margin (+ margin title-size line-gap) body-size DARKGRAY)
    (doseq [card (:cards layout)]
      (draw-rectangle! card card-color RAYWHITE)
      (draw-text (get-in (gallery/scene-by-id scene-registry (:scene-id card))
                         [:title])
                 (+ (:x card) margin)
                 (+ (:y card) (quot (:height card) 2) (- (quot body-size 2)))
                 body-size RAYWHITE))
    (let [footer-y (- height (+ margin body-size))]
      (draw-text "Android Back: close | scene Back: gallery | mouse fallback"
                 margin (- footer-y line-gap) body-size DARKGRAY)
      (draw-text (str "Frame " frame " | six registered scenes | cards " (:columns layout)
                      "x" (:rows layout))
                 margin footer-y body-size DARKGRAY))
    (end-drawing)))

(defn- draw-gesture-diagnostics! [scene-state back sizes]
  (let [{:keys [last-code point log]} scene-state
        {:keys [margin title-size body-size line-gap]} sizes]
    (clear-background RAYWHITE)
    (draw-rectangle! back CARD-DARK RAYWHITE)
    (draw-text "< Back to gallery" (+ (:x back) (quot margin 2)) (+ (:y back) (quot body-size 3)) body-size RAYWHITE)
    (draw-text "Gesture Diagnostics" margin (+ margin title-size line-gap) title-size CARD-DARK)
    (draw-text "Raylib rgestures observes at most two touch points" margin (+ margin (* 2 line-gap) title-size) body-size DARKGRAY)
    (draw-text (str "Current code: " last-code " | point-zero: " point) margin (+ margin (* 3 line-gap) title-size) body-size DARKGRAY)
    (doseq [[index entry] (map-indexed vector log)]
      (draw-text entry margin (+ margin (* (+ 4 index) line-gap) title-size) body-size (if (= index (dec (count log))) MAROON DARKGRAY)))))

(defn- draw-touch-diagnostics! [scene-state back sizes]
  (let [{:keys [count ids point-0 coordinates all-coordinates-available? phase]} scene-state
        {:keys [margin title-size body-size line-gap]} sizes]
    (clear-background RAYWHITE)
    (draw-rectangle! back CARD-DARK RAYWHITE)
    (draw-text "< Back to gallery" (+ (:x back) (quot margin 2))
               (+ (:y back) (quot body-size 3)) body-size RAYWHITE)
    (draw-text "Touch Diagnostics" margin (+ margin title-size line-gap) title-size CARD-DARK)
    (draw-text (str "Active points: " count " | phase: " (name phase))
               margin (+ margin (* 2 line-gap) title-size) body-size DARKGRAY)
    (draw-text (str "IDs: " ids) margin (+ margin (* 3 line-gap) title-size) body-size DARKGRAY)
    (draw-text (str "Point zero: " point-0 " | scalar availability: " coordinates)
               margin (+ margin (* 4 line-gap) title-size) body-size DARKGRAY)
    (draw-text (str "All point coordinates: " (if all-coordinates-available? "available" "unavailable"))
               margin (+ margin (* 5 line-gap) title-size) body-size MAROON)))

(defn- draw-touch-trail! [input scene-state back sizes]
  (let [{:keys [radius]} (trail/layout (:metrics input))
        {:keys [margin title-size body-size line-gap]} sizes
        points (:points scene-state)
        point-count (max 1 (count points))]
    (clear-background CARD-DARK)
    (draw-rectangle! back CARD-BLUE RAYWHITE)
    (draw-text "< Back to gallery" (+ (:x back) (quot margin 2))
               (+ (:y back) (quot body-size 3)) body-size RAYWHITE)
    (draw-text "Touch Trail" margin (+ margin title-size line-gap) title-size RAYWHITE)
    (draw-text (str "Drag to paint | " (count points) "/" trail/max-points " points")
               margin (+ margin (* 2 line-gap) title-size) body-size RAYWHITE)
    (doseq [[index [x y]] (map-indexed vector points)]
      (let [fade (/ (double (inc index)) point-count)
            color (rgba 64 (int (+ 120 (* 110 fade))) 255 (int (+ 70 (* 185 fade))))]
        (draw-circle (int x) (int y) (* radius fade) color)))))

(defn- draw-following-eyes! [input scene-state back sizes]
  (let [{:keys [left right eye-radius pupil-radius]} (eyes/layout (:metrics input))
        target (:target scene-state)
        left-pupil (eyes/pupil left eye-radius pupil-radius target)
        right-pupil (eyes/pupil right eye-radius pupil-radius target)
        {:keys [margin title-size body-size line-gap]} sizes]
    (clear-background RAYWHITE)
    (draw-rectangle! back CARD-DARK RAYWHITE)
    (draw-text "< Back to gallery" (+ (:x back) (quot margin 2))
               (+ (:y back) (quot body-size 3)) body-size RAYWHITE)
    (draw-text "Following Eyes" margin (+ margin title-size line-gap)
               title-size CARD-DARK)
    (draw-text "Touch and drag anywhere; release retains the last look"
               margin (+ margin (* 2 line-gap) title-size) body-size DARKGRAY)
    (doseq [[eye pupil] [[left left-pupil] [right right-pupil]]]
      (draw-circle (int (first eye)) (int (second eye)) (double eye-radius) LIGHTGRAY)
      (draw-circle (int (first pupil)) (int (second pupil)) (double pupil-radius) CARD-DARK))))

(defn- draw-flappy-bird! [frame input scene-state back sizes]
  (let [{:keys [height bird-x bird-radius pipe-width gap-height]}
        (flappy/dimensions (:metrics input))
        {:keys [margin title-size body-size line-gap]} sizes]
    (clear-background SKYBLUE)
    (draw-rectangle! back CARD-DARK RAYWHITE)
    (draw-text "< Back to gallery" (+ (:x back) (quot margin 2))
               (+ (:y back) (quot body-size 3)) body-size RAYWHITE)
    (draw-text "Flappy Bird" margin (+ margin title-size line-gap)
               title-size RAYWHITE)
    (draw-text (str "Touch or Enter to flap | score " (:score scene-state))
               margin (+ margin (* 2 line-gap) title-size) body-size DARKGRAY)
    (doseq [{:keys [x gap]} (:pipes scene-state)]
      (draw-rectangle (int x) 0 (int pipe-width) (int gap) DARKGREEN)
      (draw-rectangle (int x) (int (+ gap gap-height)) (int pipe-width)
                      (int (- height (+ gap gap-height))) DARKGREEN))
    (draw-circle (int bird-x) (int (:y scene-state)) (double bird-radius) GOLD)
    (when (:over? scene-state)
      (draw-text "GAME OVER - TOUCH TO RESTART" margin (int (/ height 2.0))
                 body-size MAROON))
    (draw-text (str "Frame " frame " | elapsed " (int (* 1000 (:elapsed scene-state))) " ms")
               margin (- (int height) (+ margin line-gap)) body-size DARKGRAY)))

(defn draw-scene! [frame input gallery-state app-state effects presentation]
  (let [metrics (:metrics input)
        {:keys [margin title-size body-size line-gap]} (diagnostics/layout metrics)
        layout (gallery-layout input)
        scene-id (:active-scene-id gallery-state)
        scene (gallery/scene-by-id scene-registry scene-id)
        scene-state (:scene-state gallery-state)
        controls (scene-controls input)
        view (reducer/view-model app-state)
        background (presentation-color presentation :background [245 245 245 255])
        accent (presentation-color presentation :accent [0 82 172 255])
        [width height] (:screen metrics)
        back (:back layout)]
    (begin-drawing)
    (cond
      (= :gesture-diagnostics scene-id)
      (draw-gesture-diagnostics! scene-state back
                                 {:margin margin :title-size title-size
                                  :body-size body-size :line-gap line-gap})
      (= :touch-diagnostics scene-id)
      (draw-touch-diagnostics! scene-state back
                               {:margin margin :title-size title-size
                                :body-size body-size :line-gap line-gap})
      (= :touch-trail scene-id)
      (draw-touch-trail! input scene-state back
                         {:margin margin :title-size title-size
                          :body-size body-size :line-gap line-gap})
      (= :following-eyes scene-id)
      (draw-following-eyes! input scene-state back
                            {:margin margin :title-size title-size
                             :body-size body-size :line-gap line-gap})
      (= :flappy-bird scene-id)
      (draw-flappy-bird! frame input scene-state back
                          {:margin margin :title-size title-size
                           :body-size body-size :line-gap line-gap})
      :else (do
        (clear-background background)
        (draw-rectangle! back CARD-DARK RAYWHITE)
        (draw-text "< Back to gallery" (+ (:x back) (quot margin 2))
                   (+ (:y back) (quot body-size 3)) body-size RAYWHITE)
        (draw-text (:title scene) margin (+ margin title-size line-gap)
                   title-size accent)
        (draw-text "Placeholder scene ready for its focused adaptation"
                   margin (+ margin (* 2 line-gap) title-size) body-size DARKGRAY)
        (draw-text (str "Scene " (name scene-id) " | frame " (:frame scene-state))
                   margin (+ margin (* 3 line-gap) title-size) body-size MAROON)
        (draw-text (str "Pointer " (name (get-in input [:pointer :phase]))
                        " | position " (get-in input [:pointer :position]))
                   margin (+ margin (* 4 line-gap) title-size) body-size DARKGRAY)
        (draw-text (str "Screen " width "x" height " | touch count "
                        (get-in input [:touches :count]))
                   margin (+ margin (* 5 line-gap) title-size) body-size DARKGRAY)
        (draw-text (str "Shared counter " (:counter view)
                        " | effect data: " (effects-label effects))
                   margin (+ margin (* 6 line-gap) title-size) body-size MAROON)
        (doseq [control controls]
          (draw-rectangle! control CARD-BLUE RAYWHITE)
          (draw-text (:label control)
                     (+ (:x control) (quot margin 2))
                     (+ (:y control) (quot body-size 3))
                     body-size RAYWHITE))
        (draw-text (str "Frame " frame " | Android Back or canvas Back returns to gallery")
                   margin (- height (+ margin line-gap)) body-size DARKGRAY)))
    (end-drawing)))

(defn- take-owner-request! []
  (loop []
    (let [before @owner-work
          [after request] (repl-queue/take-next before)]
      (cond
        (nil? request) nil
        (compare-and-set! owner-work before after) request
        :else (recur)))))

(defn- process-one-owner-request! []
  (when-let [{:keys [id work]} (take-owner-request!)]
    (let [result (try
                   {:status :ok :value (work)}
                   (catch Throwable error
                     {:status :error :message (str error)}))]
      (swap! owner-work repl-queue/complete id result))))

(defn- advance-gallery [gallery-state input]
  (let [layout (gallery-layout input)
        phase (get-in input [:pointer :phase])
        point (get-in input [:pointer :position])
        hit (gallery-ui/hit-test layout point (:mode gallery-state))
        canvas-back? (and (= :scene (:mode gallery-state)) (= :back hit))
        input (assoc input :back? (or (:back? input) canvas-back?))]
    (if (= :gallery (:mode gallery-state))
      (if (and (not (:back? input)) (= :press phase) hit)
        (gallery/open-scene scene-registry gallery-state hit input)
        (gallery/run-frame scene-registry gallery-state input))
      (gallery/run-frame scene-registry gallery-state input))))


(defn run-loop []
  (let [duration-ms 905000
        target-fps 30
        start (System/currentTimeMillis)]
    (init-window 0 0 "Jolt Raylib Gallery")
    (set-target-fps target-fps)
    (try
      (loop [frame 0
             diagnostic-state diagnostics/initial-state
             gallery-state gallery/initial-gallery-state
             app-state reducer/initial-state
             last-effects []]
        (let [elapsed (- (System/currentTimeMillis) start)
              input (assoc (poll-input) :delta-seconds (get-frame-time))
              _ (process-one-owner-request!)
              event (app/counter-event (:mode gallery-state) input
                                        (scene-controls input))
              [next-app-state emitted-effects] (app/step app-state event)
              effects (if event emitted-effects last-effects)
              next-diagnostic-state (diagnostics/step diagnostic-state input)
              next-gallery-state (advance-gallery gallery-state input)
              presentation (gallery-ui/live-presentation)
              phase (get-in input [:pointer :phase])
              navigation? (or (not= (:mode gallery-state) (:mode next-gallery-state))
                              (not= (:active-scene-id gallery-state)
                                    (:active-scene-id next-gallery-state)))]
          (when (zero? (mod frame 30))
            (reset! runtime-state
                    {:status :running
                     :frame frame
                     :owner-thread-id (.getId (Thread/currentThread))
                     :presentation (:revision presentation)
                     :gallery-mode (:mode next-gallery-state)
                     :selected-scene (:active-scene-id next-gallery-state)
                     :scene-state (when-let [scene-state (:scene-state next-gallery-state)]
                                    (select-keys scene-state [:elapsed :score :over? :y :vy :target :phase :points :last-code :point :log]))}))
          (when (or (zero? (mod frame 150))
                    (not= :idle phase)
                    (:back? input)
                    navigation?
                    event)
            (log-state! frame input next-diagnostic-state next-gallery-state
                        next-app-state effects event))
          (if (or (>= elapsed duration-ms)
                  (:close-requested? next-gallery-state))
            frame
            (do
              (if (= :gallery (:mode next-gallery-state))
                (draw-gallery! frame input presentation)
                (draw-scene! frame input next-gallery-state next-app-state effects
                             presentation))
              (recur (inc frame) next-diagnostic-state next-gallery-state
                     next-app-state effects)))))
      (finally
        (reset! runtime-state
                (assoc @runtime-state :status :stopped))
        (close-window)))))

;; Preserve the persistent-loop export consumed by the existing NativeActivity
;; bootstrap while also giving gallery-specific tooling a descriptive symbol.
(ffi/export! "raylib_persistent_loop" run-loop [] :int)
(ffi/export! "raylib_gallery" run-loop [] :int)
