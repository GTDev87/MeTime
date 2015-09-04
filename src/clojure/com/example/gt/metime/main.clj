(ns com.example.gt.metime.main
  (:require [neko.activity :refer [defactivity set-content-view!]]
            [neko.debug :refer [*a]]
            [neko.ui :refer [config make-ui]]
            [neko.log :as log]
            [neko.ui.adapters :as adapters]
            [clojure.string :refer [join]]
            [neko.find-view :refer [find-view]]
            [neko.threading :refer [on-ui]])
  (:import [android.widget CursorAdapter TextView LinearLayout]
           android.graphics.Color
           [android.os SystemClock Handler]
           java.util.Calendar
           [android.app Activity DialogFragment]
           android.view.View
           [android.app DatePickerDialog DatePickerDialog$OnDateSetListener]
           [android.app TimePickerDialog TimePickerDialog$OnTimeSetListener]))

(declare add-event)
(declare date-picker)
(declare time-picker)

;(def my-handler (Handler.))

(defrecord Task [date name duration date-index])

(defn show-picker [activity picker picker-type]
  (. picker show (. activity getFragmentManager) picker-type))

(def listing (atom (sorted-map)))

(defn sorted-map-array-to-array-task [lst]
  (mapcat identity (map (fn [sorted-map-data]
                          (into [] (map-indexed (fn [i ele] (apply ->Task (concat [(first sorted-map-data)] ele [i]))) (second sorted-map-data))))
                        lst)))

(defn create-update-timer-method [event-timer-view start-time time-swap ]
  (defn update-timer-method []
    (let [time-in-millis (- (SystemClock/uptimeMillis) start-time)
          final-time (+ time-swap time-in-millis)
          total-seconds (mod final-time 1000)
          minutes (quot total-seconds 1000)
          seconds (mod total-seconds 60)
          milliseconds (mod final-time 1000)]
      (config event-timer-view :text (str "" minutes ":" (format "%02d" seconds) ":" (format "%02d" milliseconds))))))

;myHandler.postDelayed(this, 0);

(defn make-date-adapter []
  (adapters/ref-adapter
    (fn [_]
      [:linear-layout {:id-holder   true
                       :orientation :vertical}
       [:text-view {:id ::date-tv}]
       [:linear-layout {:id ::event-ll
                        :id-holder true
                        :orientation :horizontal}
        [:text-view {:id ::event-tv}]
        [:text-view {:id ::time-tv}]
        [:button {:id ::event-btn
                  :text  "start",
                  ;:on-click
                  }]
        ]])
    (fn [_ view _ task]
      (let [date-text-view (find-view view ::date-tv)
            event-linear-layout-view (find-view view ::event-ll)
            event-text-view (find-view event-linear-layout-view ::event-tv)
            event-timer-view (find-view event-linear-layout-view ::time-tv)
            event-button-view (find-view event-linear-layout-view ::event-btn)
            my-handler (Handler.)]
        ;symptom of mutability
        (config date-text-view :visibility (if (= (:date-index task) 0) View/VISIBLE View/GONE))
        (config date-text-view :text (str (:date task)))

        (config event-button-view :on-click (fn [_] (.postDelayed my-handler (create-update-timer-method event-timer-view 0 0) 0)))
        (config event-text-view :text (str (:name task) " " (:duration task) " " (:date-index task)))))
    listing
    sorted-map-array-to-array-task))

(defn main-layout [activity]
  [:linear-layout {:orientation :vertical}
   [:edit-text {
                :hint "Event name",
                :id   ::name}]
   [:linear-layout {:orientation :horizontal}
    [:text-view {:hint "Goal (Time)",
                 :id   ::time}]
    [:button {:text     "...",
              :on-click (fn [_] (show-picker activity
                                             (time-picker activity), "timePicker"))}]]
   [:linear-layout {:orientation :horizontal}
    [:text-view {:hint "Event date",
                 :id   ::date}]
    [:button {:text     "...",
              :on-click (fn [_] (show-picker activity
                                             (date-picker activity) "datePicker"))}]]
   [:button {:text     "+ Event",
             :on-click (fn [_] (add-event activity))}]
   [:list-view {:id                   ::days
                :draw-selector-on-top true
                :adapter              (make-date-adapter)}]])

(defn get-elmt [activity elmt]
  (str (.getText ^TextView (find-view activity elmt))))

(defn set-elmt-text [activity elmt s]
  (on-ui (config (find-view activity elmt) :text s)))



(defn update-ui [activity]
  (set-elmt-text activity ::name ""))

(defn add-to-listing [event-name date-key time-key]
  (swap! listing update-in [date-key] (fnil conj []) [event-name time-key]))

(defn add-event [activity]
  (let [date-key (try
                   (read-string (get-elmt activity ::date))
                   (catch RuntimeException e "Date string is empty!"))
        time-key (try
                   (read-string (get-elmt activity ::time))
                   (catch RuntimeException e "Time string is empty!"))]
    (when (number? date-key)
      (add-to-listing (get-elmt activity ::name) date-key time-key)
      (update-ui activity))))

(defn date-picker [activity]
  (proxy [DialogFragment DatePickerDialog$OnDateSetListener] []
    (onCreateDialog [_]
      (let [c (Calendar/getInstance)
            year (.get c Calendar/YEAR)
            month (.get c Calendar/MONTH)
            day (.get c Calendar/DAY_OF_MONTH)]
        (DatePickerDialog. activity this year month day)))
    (onDateSet [_ year month day]
      (set-elmt-text activity ::date
                     (format "%d%02d%02d" year (inc month) day)))))

(defn time-picker [activity]
  (proxy [DialogFragment TimePickerDialog$OnTimeSetListener] []
    (onCreateDialog [_]
      (TimePickerDialog. activity this 0 0 true))
    (onTimeSet [_ hourOfDay minute]
      (set-elmt-text activity ::time
                     (format "%d" (+ (* hourOfDay 60) minute))))))

(defactivity com.example.gt.metime.MainActivity
             :key :main
             (onCreate [this bundle]
                       (.superOnCreate this bundle)
                       (on-ui (set-content-view! (*a) (main-layout (*a))))))