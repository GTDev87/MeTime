(ns com.gt.metime.listing)

; interacting with listing atom
(def listing (atom (sorted-map)))

(defn add-to-listing [event-name date-key time-key]
  (swap! listing update-in [date-key] (fnil conj []) [event-name time-key]))

(defn remove-element-from-vec [date-list index]
  (vec (concat (subvec date-list 0 index) (subvec date-list (+ index 1)))))

(defn remove-from-listing [date-key date-index]
  (swap! listing update-in [date-key] (fn [date-list] (remove-element-from-vec date-list date-index))))
; end interacting with listing atom

; parsing listing
(defrecord Task [date name duration date-index])

(defn sorted-map-array-to-array-task [lst]
  (mapcat
    identity
    (map
      (fn [sorted-map-data]
        (into [] (map-indexed (fn [i ele] (apply ->Task (concat [(first sorted-map-data)] ele [i]))) (second sorted-map-data))))
      lst)))
; end parsint listing