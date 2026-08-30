(ns poc.raylib.touch-diagnostics "Pure diagnostic view model for normalized Raylib touch input.")
(defn ^:export view [input]
  {:count (get-in input [:touches :count]) :ids (get-in input [:touches :ids])
   :point-0 (get-in input [:touches :point-0])
   :coordinates (get-in input [:touches :available-coordinates])
   :all-coordinates-available? (get-in input [:touches :all-coordinates-available?])
   :phase (get-in input [:pointer :phase])})
(defn ^:export scene []
  {:id :touch-diagnostics :title "Touch Diagnostics"
   :init (fn [input] [(view input) [[:scene/init :touch-diagnostics]]])
   :update (fn [_ input] [(view input) []]) :draw (fn [state _] [state []])
   :dispose (fn [state] [state [[:scene/dispose :touch-diagnostics]]])})
