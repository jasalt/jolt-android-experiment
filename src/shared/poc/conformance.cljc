(ns poc.conformance
  "Host-neutral runner for the tab-separated canonical wire corpus."
  (:require [clojure.string :as str]))

(def default-fixtures "test/conformance/fixtures.tsv")

(defn fixture-rows
  ([] (fixture-rows default-fixtures))
  ([path]
   (->> (str/split-lines (clojure.core/slurp path))
        (remove #(or (str/blank? %) (str/starts-with? % "#")))
        (map #(let [[id event expected] (str/split % #"\t" 3)]
                {:id id :event event :expected expected})))))

(defn verify!
  "Calls a stateful host wire-dispatch function for every shared corpus row.
  Returns the checked row IDs, or throws with the exact divergent payload."
  [dispatch-wire!]
  (mapv (fn [{:keys [id event expected]}]
          (let [actual (dispatch-wire! event)]
            (when-not (= expected actual)
              (throw (ex-info "conformance fixture diverged"
                              {:id id :event event :expected expected :actual actual})))
            id))
        (fixture-rows)))
