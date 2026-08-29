(ns poc.contracts
  "Portable event/effect/capability vocabulary. Values crossing a host boundary
  are EDN-compatible data; this namespace never imports platform APIs.")

(def platform-capabilities
  {:android #{:clipboard :persistence :open-uri :notifications :vibration}
   :linux #{:clipboard :persistence :open-uri}
   :cli #{:persistence :open-uri}})

(defn capabilities [platform]
  (get platform-capabilities platform #{}))

(defn supports? [platform capability]
  (contains? (capabilities platform) capability))

(defn platform-description [platform]
  {:platform platform :capabilities (capabilities platform)})

(defn effect-capability [{:keys [type]}]
  (case type
    :platform/clipboard :clipboard
    :storage/write :persistence
    :platform/open-uri :open-uri
    :notification/show :notifications
    :platform/vibrate :vibration
    :permission/request :notifications
    nil))

(defn permitted-effect? [platform effect]
  (let [capability (effect-capability effect)]
    (or (nil? capability) (supports? platform capability))))
