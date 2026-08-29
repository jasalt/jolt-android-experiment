(ns poc.reducer)

(def initial-state
  {:counter 0
   :events []
   :platform nil
   :lifecycle nil})

(defn step [state event]
  (case (:type event)
    :counter/inc
    (let [model (update state :counter inc)]
      [model [{:type :storage/write :key "counter" :value (:counter model)}]])

    :counter/dec
    (let [model (update state :counter dec)]
      [model [{:type :storage/write :key "counter" :value (:counter model)}]])

    :counter/reset
    (let [model (assoc state :counter 0)]
      [model [{:type :storage/write :key "counter" :value 0}]])

    :storage/restore
    [(assoc state :counter (:value event)) []]

    :platform/info
    [(assoc state :platform (:value event)) []]

    :platform/copy-counter
    [state [{:type :platform/clipboard
             :text (str "Jolt counter: " (:counter state))}]]

    :lifecycle/create
    [(assoc state :lifecycle :created) []]

    :lifecycle/start
    [(assoc state :lifecycle :started) []]

    :lifecycle/resume
    [(assoc state :lifecycle :resumed) []]

    [state []]))
