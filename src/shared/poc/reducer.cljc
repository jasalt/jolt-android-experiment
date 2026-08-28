(ns poc.reducer)

(def initial-state
  {:counter 0
   :events []
   :platform nil})

(defn step [state event]
  (case (:type event)
    :counter/inc
    [(update state :counter inc) []]

    :counter/dec
    [(update state :counter dec) []]

    :counter/reset
    [(assoc state :counter 0) []]

    :platform/info
    [(assoc state :platform (:value event)) []]

    [state []]))
