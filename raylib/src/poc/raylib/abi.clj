(ns poc.raylib.abi
  "Direct Jolt FFI declarations for representative pinned Raylib aggregates.

  Layout descriptors are deliberately literal at each FFI boundary: Jolt's
  by-value macro consumes them at compile time. No pointer workaround is used."
  (:require [jolt.ffi :as ffi]))

(def color-layout (ffi/layout [:struct [[:r :uint8] [:g :uint8] [:b :uint8] [:a :uint8]]]))
(def vector2-layout (ffi/layout [:struct [[:x :float] [:y :float]]]))
(def vector3-layout (ffi/layout [:struct [[:x :float] [:y :float] [:z :float]]]))
(def rectangle-layout (ffi/layout [:struct [[:x :float] [:y :float] [:width :float] [:height :float]]]))
(def camera2d-layout (ffi/layout [:struct [[:offset [:struct [[:x :float] [:y :float]]]]
                                           [:target [:struct [[:x :float] [:y :float]]]]
                                           [:rotation :float] [:zoom :float]]]))
(def camera3d-layout (ffi/layout [:struct [[:position [:struct [[:x :float] [:y :float] [:z :float]]]]
                                           [:target [:struct [[:x :float] [:y :float] [:z :float]]]]
                                           [:up [:struct [[:x :float] [:y :float] [:z :float]]]]
                                           [:fovy :float] [:projection :int]]]))
(def texture2d-layout (ffi/layout [:struct [[:id :uint] [:width :int] [:height :int] [:mipmaps :int] [:format :int]]]))

(ffi/defcfn layout-ok "jolt_raylib_abi_layout_ok" [] :int)
(ffi/defcfn color-score "jolt_raylib_abi_color" [[:by-value [:struct [[:r :uint8] [:g :uint8] [:b :uint8] [:a :uint8]]]]] :int)
(ffi/defcfn vector2-score "jolt_raylib_abi_vector2" [[:by-value [:struct [[:x :float] [:y :float]]]]] :float)
(ffi/defcfn vector3-score "jolt_raylib_abi_vector3" [[:by-value [:struct [[:x :float] [:y :float] [:z :float]]]]] :float)
(ffi/defcfn rectangle-score "jolt_raylib_abi_rectangle" [[:by-value [:struct [[:x :float] [:y :float] [:width :float] [:height :float]]]]] :float)
(ffi/defcfn camera2d-score "jolt_raylib_abi_camera2d" [[:by-value [:struct [[:offset [:struct [[:x :float] [:y :float]]]] [:target [:struct [[:x :float] [:y :float]]]] [:rotation :float] [:zoom :float]]]]] :float)
(ffi/defcfn camera3d-score "jolt_raylib_abi_camera3d" [[:by-value [:struct [[:position [:struct [[:x :float] [:y :float] [:z :float]]]] [:target [:struct [[:x :float] [:y :float] [:z :float]]]] [:up [:struct [[:x :float] [:y :float] [:z :float]]]] [:fovy :float] [:projection :int]]]]] :float)
(ffi/defcfn texture-score "jolt_raylib_abi_texture" [[:by-value [:struct [[:id :uint] [:width :int] [:height :int] [:mipmaps :int] [:format :int]]]]] :int)
(ffi/defcfn make-vector2 "jolt_raylib_abi_make_vector2" [:float :float] [:by-value [:struct [[:x :float] [:y :float]]]])
(ffi/defcfn make-texture "jolt_raylib_abi_make_texture" [:uint :int :int :int :int] [:by-value [:struct [[:id :uint] [:width :int] [:height :int] [:mipmaps :int] [:format :int]]]])

(def expected-layouts
  {:color [4 {:r 0 :a 3}]
   :vector2 [8 {:x 0 :y 4}]
   :vector3 [12 {:z 8}]
   :rectangle [16 {:width 8 :height 12}]
   :camera2d [24 {[:offset :x] 0 [:target :x] 8 :rotation 16 :zoom 20}]
   :camera3d [44 {[:target :x] 12 [:up :x] 24 :fovy 36 :projection 40}]
   :texture2d [20 {:id 0 :format 16}]})

(def ^:dynamic *observe-stage!* (fn [_] nil))

(defn layout-observation []
  (let [observe *observe-stage!*
        checked (fn [name f] (observe (str "layout-" name)) (f))]
    {:color (checked "color" #(vector (ffi/layout-size color-layout) {:r (ffi/field-offset color-layout :r) :a (ffi/field-offset color-layout :a)}))
     :vector2 (checked "vector2" #(vector (ffi/layout-size vector2-layout) {:x (ffi/field-offset vector2-layout :x) :y (ffi/field-offset vector2-layout :y)}))
     :vector3 (checked "vector3" #(vector (ffi/layout-size vector3-layout) {:z (ffi/field-offset vector3-layout :z)}))
     :rectangle (checked "rectangle" #(vector (ffi/layout-size rectangle-layout) {:width (ffi/field-offset rectangle-layout :width) :height (ffi/field-offset rectangle-layout :height)}))
     :camera2d (checked "camera2d" #(vector (ffi/layout-size camera2d-layout) {[:offset :x] (ffi/field-offset camera2d-layout [:offset :x]) [:target :x] (ffi/field-offset camera2d-layout [:target :x]) :rotation (ffi/field-offset camera2d-layout :rotation) :zoom (ffi/field-offset camera2d-layout :zoom)}))
     :camera3d (checked "camera3d" #(vector (ffi/layout-size camera3d-layout) {[:target :x] (ffi/field-offset camera3d-layout [:target :x]) [:up :x] (ffi/field-offset camera3d-layout [:up :x]) :fovy (ffi/field-offset camera3d-layout :fovy) :projection (ffi/field-offset camera3d-layout :projection)}))
     :texture2d (checked "texture2d" #(vector (ffi/layout-size texture2d-layout) {:id (ffi/field-offset texture2d-layout :id) :format (ffi/field-offset texture2d-layout :format)}))}))

(defn write-fields! [pointer layout fields]
  (doseq [[field value] fields] (ffi/write-field pointer layout field value))
  pointer)

(defn aggregate-observation []
  (let [color (ffi/alloc (ffi/layout-size color-layout))
        v2 (ffi/alloc (ffi/layout-size vector2-layout))
        v3 (ffi/alloc (ffi/layout-size vector3-layout))
        rect (ffi/alloc (ffi/layout-size rectangle-layout))
        camera2 (ffi/alloc (ffi/layout-size camera2d-layout))
        camera3 (ffi/alloc (ffi/layout-size camera3d-layout))
        texture (ffi/alloc (ffi/layout-size texture2d-layout))
        returned-v2 (ffi/alloc (ffi/layout-size vector2-layout))
        returned-texture (ffi/alloc (ffi/layout-size texture2d-layout))]
    (try
      (write-fields! color color-layout {:r 1 :g 2 :b 3 :a 4})
      (write-fields! v2 vector2-layout {:x 1.0 :y 2.0})
      (write-fields! v3 vector3-layout {:x 1.0 :y 2.0 :z 3.0})
      (write-fields! rect rectangle-layout {:x 1.0 :y 2.0 :width 3.0 :height 4.0})
      (write-fields! camera2 camera2d-layout {[:offset :x] 1.0 [:offset :y] 2.0 [:target :x] 3.0 [:target :y] 4.0 :rotation 5.0 :zoom 6.0})
      (write-fields! camera3 camera3d-layout {[:position :x] 1.0 [:target :y] 2.0 [:up :z] 3.0 :fovy 4.0 :projection 5})
      (write-fields! texture texture2d-layout {:id 1 :width 2 :height 3 :mipmaps 4 :format 5})
      (let [score (fn [stage f] (*observe-stage!* stage) (f))]
        {:c-oracle (layout-ok)
         :color (score "color" #(color-score color))
         :vector2 (score "vector2" #(vector2-score v2))
         :vector3 (score "vector3" #(vector3-score v3))
         :rectangle (score "rectangle" #(rectangle-score rect))
         :camera2d (score "camera2d" #(camera2d-score camera2))
         :camera3d (score "camera3d" #(camera3d-score camera3))
         :texture2d (score "texture2d" #(texture-score texture))
         :return-vector2 {:destination? (= returned-v2 (score "return-vector2" #(make-vector2 returned-v2 7.0 8.0)))
                          :fields [(ffi/read-field returned-v2 vector2-layout :x)
                                   (ffi/read-field returned-v2 vector2-layout :y)]}
         :return-texture {:destination? (= returned-texture (score "return-texture" #(make-texture returned-texture 7 8 9 10 11)))
                          :fields [(ffi/read-field returned-texture texture2d-layout :id)
                                   (ffi/read-field returned-texture texture2d-layout :width)
                                   (ffi/read-field returned-texture texture2d-layout :height)
                                   (ffi/read-field returned-texture texture2d-layout :mipmaps)
                                   (ffi/read-field returned-texture texture2d-layout :format)]}})
      (finally
        (doseq [pointer [returned-texture returned-v2 texture camera3 camera2 rect v3 v2 color]]
          (ffi/free pointer))))))

(def expected-aggregates
  {:c-oracle 1 :color 4321 :vector2 21.0 :vector3 321.0 :rectangle 4321.0
   :camera2d 21.0 :camera3d 15.0 :texture2d 15
   :return-vector2 {:destination? true :fields [7.0 8.0]}
   :return-texture {:destination? true :fields [7 8 9 10 11]}})

(defn verify! []
  (let [_ (*observe-stage!* "layouts")
        layouts (layout-observation)
        _ (*observe-stage!* "aggregates")
        aggregates (aggregate-observation)
        observation {:layouts layouts :aggregates aggregates}]
    (when-not (= expected-layouts (:layouts observation))
      (throw (ex-info "Unexpected Jolt Raylib aggregate layout" observation)))
    (when-not (= expected-aggregates (:aggregates observation))
      (throw (ex-info "Unexpected Jolt Raylib aggregate ABI result" observation)))
    observation))
