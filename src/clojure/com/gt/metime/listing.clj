(ns com.gt.metime.listing
  (:import [android.os SystemClock]))

; interacting with listing atom
(def listing (atom (sorted-map)))
(defrecord TimerContext [running offset base])

;metime stuff
(defn initial-me-time-listing []
  [["Me Time" (* 24 60) (atom (apply ->TimerContext [false 0 (SystemClock/elapsedRealtime)])) true]])

(defn is-metime? [arr-element]
  (nth arr-element 3))
;end metime stuff

(defn indices [pred coll]
  (keep-indexed #(when (pred %2) %1) coll))

(defn calculate-me-time [array]
  (let [total-minutes (reduce (fn [agg ele] (+ (nth ele 1) agg)) 0 (filter (complement is-metime?) array))
        me-time-index (first (indices is-metime? array))
        element (nth array me-time-index)]
    (assoc array me-time-index (assoc element 1 (- (* 24 60) total-minutes)))))

(defn fn-with-calculate-me-time [the-fn]
  (fn [array element] (calculate-me-time (the-fn array element))))

(defn add-to-listing [event-name date-key time-key]
  (swap! listing update-in [date-key] (fnil (fn-with-calculate-me-time conj) (initial-me-time-listing)) [event-name time-key (atom (apply ->TimerContext [false 0 (SystemClock/elapsedRealtime)])) false]))

(defn remove-element-from-vec [date-list index]
  (calculate-me-time (vec (concat (subvec date-list 0 index) (subvec date-list (+ index 1))))))

(defn remove-from-listing [date-key date-index]
  (swap! listing update-in [date-key] (fn [date-list] (remove-element-from-vec date-list date-index))))
; end interacting with listing atom

; parsing listing
(defrecord Task [date name duration timer-context me-time date-index])

(defn sorted-map-array-to-array-task [lst]
  (mapcat
    identity
    (map
      (fn [sorted-map-data]
        (into [] (map-indexed (fn [i ele] (apply ->Task (concat [(first sorted-map-data)] ele [i]))) (second sorted-map-data))))
      lst)))
; end parsint listing