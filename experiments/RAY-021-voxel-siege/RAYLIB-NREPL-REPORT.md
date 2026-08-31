# Linux Raylib nREPL report

A pinned Linux Jolt nREPL server was started on loopback port 7899 from the
`raylib/` project. `brepl` connected successfully and evaluated:

```clojure
(require 'poc.raylib.gallery-ui)
(select-keys (poc.raylib.gallery-ui/live-presentation) [:revision :title])
;; {:revision :baseline, :title "Jolt + Raylib Gallery"}
```

A first qualified `defn` form did not alter the already-loaded presentation
Var in this server. Using the explicit Var-root operation
`(alter-var-root (var poc.raylib.gallery-ui/live-presentation) (constantly
(fn [] {:revision :repl :title "Replaced"})))` then returned
`{:revision :repl, :title "Replaced"}`. The safe boundary is therefore: pure
Var redefinition works through loopback nREPL, while graphical Raylib calls
must remain on the owner thread and still require a graphical Linux display.
No unsafe cross-thread Raylib call or editor/CIDER claim is made.

[Raylib Android scene visual reference (screenshot)](evidence/voxel-scene-live.png)

The embedded image is a direct host visual reference, not a Linux nREPL
screenshot. Linux graphical capture remains blocked by the recorded Xvfb GLX
configuration.
