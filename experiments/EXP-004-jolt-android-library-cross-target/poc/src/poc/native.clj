(ns poc.native
  (:require [clojure.edn :as edn]
            [jolt.ffi :as ffi]
            [poc.reducer :as reducer]))

(def app-state (atom reducer/initial-state))

(defn answer [] 42)

(defn allocate [n]
  (loop [i 0 values []]
    (if (= i n) (count values) (recur (+ i 1) (conj values i)))))

(defn dispatch-counter [event-edn]
  (let [event (edn/read-string event-edn)
        [model _] (reducer/step @app-state event)]
    (reset! app-state model)
    (:counter model)))

(defn lifecycle-code []
  (case (:lifecycle @app-state)
    :created 1
    :started 2
    :resumed 3
    0))

(defn effect-code [event-edn]
  (let [event (edn/read-string event-edn)
        [_ effects] (reducer/step @app-state event)]
    (case (:type (first effects))
      :platform/clipboard 1
      :storage/write 2
      :permission/request 3
      :platform/vibrate 4
      :platform/open-uri 5
      :platform/read-info 6
      :notification/show 7
      0)))

(defn worker-code []
  (if (= :completed (:worker @app-state)) 1 0))

(defn permission-code []
  (case (:notification-permission @app-state)
    :granted 1
    :denied 2
    0))

(defn debug-eval [source]
  ;; This debug-only function is serialized by JoltRuntime. `:string` causes
  ;; Jolt's export layer to copy the result to C rather than exposing managed
  ;; memory. Results/errors are bounded before the JNI bridge creates a String.
  (try
    (let [result (pr-str (load-string source))]
      (if (> (count result) 65536)
        "{:error {:type :eval/result-too-large}}"
        (str "{:ok " result "}")))
    (catch :default error
      (str "{:error {:type :eval/failed :message "
           (pr-str (str error)) "}}"))))

(ffi/export! "poc_answer" answer [] :int)
(ffi/export! "poc_allocate" allocate [:int] :int)
(ffi/export! "poc_dispatch_counter" dispatch-counter [:string] :int)
(ffi/export! "poc_lifecycle_code" lifecycle-code [] :int)
(ffi/export! "poc_effect_code" effect-code [:string] :int)
(ffi/export! "poc_worker_code" worker-code [] :int)
(ffi/export! "poc_permission_code" permission-code [] :int)
(ffi/export! "poc_debug_eval" debug-eval [:string] :string)
