(ns poc.wire
  "Canonical, data-only EDN boundary. Hosts supply and own strings/buffers."
  (:require [clojure.edn :as edn]))

(def max-input-bytes (* 1024 1024))
(def max-output-bytes (* 1024 1024))

(defn- byte-count [s] (count (.getBytes s "UTF-8")))
(defn- error [kind message] {:error {:type kind :message message}})

(defn decode-event [wire]
  (cond
    (not (string? wire)) (error :wire/invalid "event must be UTF-8 text")
    (> (byte-count wire) max-input-bytes) (error :wire/too-large "event exceeds 1 MiB")
    :else (try
            (let [value (edn/read-string wire)]
              (if (and (map? value) (keyword? (:type value)))
                {:ok value}
                (error :wire/invalid "event must be a map with keyword :type")))
            (catch :default _ (error :wire/malformed "invalid EDN event")))))

(defn encode-response [response]
  (let [wire (pr-str response)]
    (if (> (byte-count wire) max-output-bytes)
      (pr-str (error :wire/too-large "response exceeds 1 MiB"))
      wire)))

(defn decode-response [wire]
  (try {:ok (edn/read-string wire)}
       (catch :default _ (error :wire/malformed "invalid EDN response"))))
