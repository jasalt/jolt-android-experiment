(ns poc.raylib.repl-queue
  "Pure bounded queue state for debug REPL work executed by the frame owner.")

(def max-pending 16)
(def max-results 64)

(def initial-state
  {:pending []
   :results {}
   :result-order []})

(defn enqueue
  "Append a request when capacity remains. Return [next-state accepted?]."
  [state request]
  (if (>= (count (:pending state)) max-pending)
    [state false]
    [(update state :pending conj request) true]))

(defn take-next
  "Return [next-state oldest-request], or [state nil] when empty."
  [state]
  (if-let [request (first (:pending state))]
    [(update state :pending #(vec (rest %))) request]
    [state nil]))

(defn complete
  "Store one bounded result and evict the oldest result past max-results."
  [state request-id result]
  (let [known? (contains? (:results state) request-id)
        order (cond-> (:result-order state) (not known?) (conj request-id))
        excess (max 0 (- (count order) max-results))
        evicted (take excess order)
        retained-order (vec (drop excess order))]
    (-> state
        (assoc :result-order retained-order)
        (assoc-in [:results request-id] result)
        (update :results #(apply dissoc % evicted)))))

(defn result [state request-id]
  (get-in state [:results request-id]))
