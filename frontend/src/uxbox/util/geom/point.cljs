;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2015-2017 Andrey Antukh <niwi@niwi.nz>
;; Copyright (c) 2015-2017 Juan de la Cruz <delacruzgarciajuan@gmail.com>

(ns uxbox.util.geom.point
  (:refer-clojure :exclude [divide])
  (:require [uxbox.util.math :as mth]
            [cognitect.transit :as t]))

;; --- Point Impl

(defrecord Point [x y])

(defn ^boolean point?
  "Return true if `v` is Point instance."
  [v]
  (instance? Point v))

(defn point
  "Create a Point instance."
  ([] (Point. 0 0))
  ([v]
   (cond
     (point? v)
     v

     (or (vector? v)
         (seq? v))
     (Point. (first v) (second v))

     (map? v)
     (Point. (:x v) (:y v))

     (number? v)
     (Point. v v)

     :else
     (throw (ex-info "Invalid arguments" {:v v}))))
  ([x y] (Point. x y)))

(defn rotate
  "Apply rotation transformation to the point."
  [p angle]
  {:pre [(point? p)]}
  (let [angle (mth/radians angle)
        sin (mth/sin angle)
        cos (mth/cos angle)]
    (Point.
     (-> (- (* (:x p) cos) (* (:y p) sin))
         (mth/precision 6))
     (-> (+ (* (:x p) sin) (* (:y p) cos))
         (mth/precision 6)))))

(defn add
  "Returns the addition of the supplied value to both
  coordinates of the point as a new point."
  [p other]
  {:pre [(point? p)]}
  (let [other (point other)]
    (Point. (+ (:x p) (:x other))
            (+ (:y p) (:y other)))))

(defn subtract
  "Returns the subtraction of the supplied value to both
  coordinates of the point as a new point."
  [p other]
  {:pre [(point? p)]}
  (let [other (point other)]
    (Point. (- (:x p) (:x other))
            (- (:y p) (:y other)))))


(defn multiply
  "Returns the subtraction of the supplied value to both
  coordinates of the point as a new point."
  [p other]
  {:pre [(point? p)]}
  (let [other (point other)]
    (Point. (* (:x p) (:x other))
            (* (:y p) (:y other)))))

(defn divide
  [p other]
  {:pre [(point? p)]}
  (let [other (point other)]
    (Point. (/ (:x p) (:x other))
            (/ (:y p) (:y other)))))

(defn negate
  [p]
  {:pre [(point? p)]}
  (let [{:keys [x y]} (point p)]
    (Point. (- x) (- y))))

(defn distance
  "Calculate the distance between two points."
  [p other]
  (let [other (point other)
        dx (- (:x p) (:x other))
        dy (- (:y p) (:y other))]
    (-> (mth/sqrt (+ (mth/pow dx 2)
                     (mth/pow dy 2)))
        (mth/precision 6))))

(defn length
  [p]
  {:pre [(point? p)]}
  (mth/sqrt (+ (mth/pow (:x p) 2)
               (mth/pow (:y p) 2))))

(defn angle
  "Returns the smaller angle between two vectors.
  If the second vector is not provided, the angle
  will be measured from x-axis."
  ([p]
   {:pre [(point? p)]}
   (-> (mth/atan2 (:y p) (:x p))
       (mth/degrees)))
  ([p center]
   (let [center (point center)]
     (angle (subtract p center)))))

(defn angle-with-other
  "Consider point as vector and calculate
  the angle between two vectors."
  [p other]
  {:pre [(point? p)]}
  (let [other (point other)
        a (/ (+ (* (:x p) (:x other))
                (* (:y p) (:y other)))
             (* (length p) (length other)))
        a (mth/acos (if (< a -1)
                      -1
                      (if (> a 1) 1 a)))]
    (-> (mth/degrees a)
        (mth/precision 6))))

(defn update-angle
  "Update the angle of the point."
  [p angle]
  (let [len (length p)
        angle (mth/radians angle)]
    (Point. (* (mth/cos angle) len)
            (* (mth/sin angle) len))))

(defn quadrant
  "Return the quadrant of the angle of the point."
  [{:keys [x y] :as p}]
  {:pre [(point? p)]}
  (if (>= x 0)
    (if (>= y 0) 1 4)
    (if (>= y 0) 2 3)))

(defn round
  "Change the precision of the point coordinates."
  [{:keys [x y]} decimanls]
  (Point. (mth/precision x decimanls)
          (mth/precision y decimanls)))

(defn transform
  "Transform a point applying a matrix transfomation."
  [pt {:keys [a b c d tx ty] :as m}]
  (let [{:keys [x y]} (point pt)]
    (Point. (+ (* x a) (* y c) tx)
            (+ (* x b) (* y d) ty))))


;; --- Transit Adapter

(def point-write-handler
  (t/write-handler
   (constantly "point")
   (fn [v] (into {} v))))

(def point-read-handler
  (t/read-handler
   (fn [value]
     (map->Point value))))
