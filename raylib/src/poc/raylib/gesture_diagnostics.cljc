(ns poc.raylib.gesture-diagnostics "Pure bounded transition log for upstream Raylib gesture codes.")
(def max-log 20)
(def names {1 "TAP" 2 "DOUBLETAP" 4 "HOLD" 8 "DRAG" 16 "SWIPE-RIGHT" 32 "SWIPE-LEFT" 64 "SWIPE-UP" 128 "SWIPE-DOWN" 256 "PINCH-IN" 512 "PINCH-OUT"})
(defn step [state input]
 (let [code (get-in input [:gesture :code]) point (get-in input [:pointer :position]) new? (and (not= 0 code) (not= code (:last-code state)))]
  (assoc state :last-code code :point point :log (if new? (vec (take-last max-log (conj (:log state) (get names code (str "GESTURE-" code))))) (:log state)))))
(defn ^:export scene [] {:id :gesture-diagnostics :title "Gesture Diagnostics" :init (fn [_] [{:last-code 0 :point nil :log []} [[:scene/init :gesture-diagnostics]]]) :update (fn [s i] [(step s i) []]) :draw (fn [s _] [s []]) :dispose (fn [s] [s [[:scene/dispose :gesture-diagnostics]]])})
