(ns poc.reducer
  (:require [poc.contracts :as contracts]))

(def ^{:clj-kondo/ignore [:clojure-lsp/unused-public-var]} initial-state
  {:counter 0
   :events []
   :platform nil
   :lifecycle nil
   :worker nil
   :notification-permission nil})

(defn ^{:clj-kondo/ignore [:clojure-lsp/unused-public-var]} view-model
  "Derived portable render data; hosts own widget construction."
  [state]
  {:counter (:counter state)
   :event-count (count (:events state))
   :lifecycle (:lifecycle state)
   :worker (:worker state)
   :notification-permission (:notification-permission state)
   :platform (:platform state)})

(defn ^{:clj-kondo/ignore [:clojure-lsp/unused-public-var]} step [state event]
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

    :worker/completed
    [(assoc state :worker :completed) []]

    :permission/request-notifications
    [state [{:type :permission/request :permission :notifications}]]

    :platform/notify-counter
    [state [{:type :notification/show :title "Jolt" :body (str "Counter: " (:counter state))}]]

    :permission/result-granted
    [(assoc state :notification-permission :granted) []]

    :permission/result-denied
    [(assoc state :notification-permission :denied) []]

    :platform/copy-counter
    [state [{:type :platform/clipboard
             :text (str "Jolt counter: " (:counter state))}]]

    :platform/vibrate
    [state [{:type :platform/vibrate :duration-ms 50}]]

    :platform/open-url
    [state [{:type :platform/open-uri :uri "https://jolt-lang.net"}]]

    :platform/read-info
    [state [{:type :platform/read-info}]]

    :platform/request-effect
    (let [effect (:effect event)
          platform (get-in state [:platform :platform])]
      (if (contracts/permitted-effect? platform effect)
        [state [effect]]
        [(assoc state :last-unsupported-effect (:type effect)) []]))

    :lifecycle/create
    [(assoc state :lifecycle :created) []]

    :lifecycle/start
    [(assoc state :lifecycle :started) []]

    :lifecycle/resume
    [(assoc state :lifecycle :resumed) []]

    :lifecycle/pause
    [(assoc state :lifecycle :paused) []]

    :lifecycle/stop
    [(assoc state :lifecycle :stopped) []]

    [state []]))
