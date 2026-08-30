(ns poc.raylib.sdk-probe
  "Single pinned-Raylib Android SDK boundary probe."
  (:require [jolt.ffi :as ffi]))

(ffi/defcfn open-url "OpenURL" [:string] :void)

(defn ^:export open-url-probe
  "Invoke Raylib OpenURL on the owner thread; caller records return/resume."
  [url]
  (if (and (string? url)
           (re-find #"^https://[A-Za-z0-9.-]+(?:/.*)?$" url))
    (do (open-url url) {:status :requested :url url})
    {:status :rejected :reason :unsafe-url}))
