(ns com.example.gt.metime.main
  (:require [neko.activity :refer [defactivity set-content-view!]]
            [neko.debug :refer [*a]]
            [neko.ui :refer [config]]
            [neko.log :as log]
            [neko.ui.adapters :as adapters]
            [clojure.string :refer [join]]
            [neko.find-view :refer [find-view]]
            [neko.threading :refer [on-ui]])
  (:import [android.widget CursorAdapter TextView]
           android.graphics.Color
           java.util.Calendar
           android.app.Activity
           [android.app DatePickerDialog DatePickerDialog$OnDateSetListener]
           [android.app TimePickerDialog TimePickerDialog$OnTimeSetListener]
           android.app.DialogFragment))

(declare add-event)
(declare date-picker)
(declare time-picker)

(defn show-picker [activity picker picker-type]
  (. picker show (. activity getFragmentManager) picker-type))

(def listing (atom (sorted-map)))

(defn make-adapter []
  (adapters/ref-adapter
    (fn [_]
      [:linear-layout {:id-holder true}
       [:text-view {:id         ::caption-tv}]])
    (fn [_ view _ date-event]
      (let [tv (find-view view ::caption-tv)]
        (config tv :text (str (nth date-event 0) " " (nth date-event 1) "\n"))))
    listing
    (fn [lst] (into [] (seq lst)))))

(defn main-layout [activity]
  [:linear-layout {:orientation :vertical}
    [:edit-text {
      :hint "Event name",
      :id ::name}]
    [:linear-layout {:orientation :horizontal}
      [:text-view {
        :hint "Goal (Time)",
        :id ::time}]
      [:button {
        :text "...",
        :on-click (fn [_] (show-picker activity
          (time-picker activity), "timePicker"))}]]
    [:linear-layout {:orientation :horizontal}
      [:text-view {
        :hint "Event date",
        :id ::date}]
      [:button {
        :text "...",
        :on-click (fn [_] (show-picker activity
          (date-picker activity) "datePicker"))}]]
    [:button {
      :text "+ Event",
      :on-click (fn [_] (add-event activity))}]
    [:list-view {
      :id ::listings
      :draw-selector-on-top true
      :adapter (make-adapter)
      ;:on-item-click (fn [_] (show-picker activity
      ;  (date-picker activity) "timerActivity"))

                 }]
   ])

(defn get-elmt [activity elmt]
  (str (.getText ^TextView (find-view activity elmt))))

(defn set-elmt-text [activity elmt s]
  (on-ui (config (find-view activity elmt) :text s)))

(defn update-ui [activity]
  (set-elmt-text activity ::name "")
  )

(defn add-event [activity]
  (let [
      date-key (try
        (read-string (get-elmt activity ::date))
        (catch RuntimeException e "Date string is empty!"))
      time-key (try
        (read-string (get-elmt activity ::time))
        (catch RuntimeException e "Time string is empty!"))]
    (when (number? date-key)
      (swap! listing update-in [date-key] (fnil conj [])
        [(get-elmt activity ::name) time-key])
      (update-ui activity))))

(defn date-picker [activity]
  (proxy [DialogFragment DatePickerDialog$OnDateSetListener] []
    (onCreateDialog [savedInstanceState]
      (let [c (Calendar/getInstance)
        year (.get c Calendar/YEAR)
        month (.get c Calendar/MONTH)
        day (.get c Calendar/DAY_OF_MONTH)]
        (DatePickerDialog. activity this year month day)))
    (onDateSet [view year month day]
      (set-elmt-text activity ::date
        (format "%d%02d%02d" year (inc month) day)))))

(defn time-picker [activity]
  (proxy [DialogFragment TimePickerDialog$OnTimeSetListener] []
    (onCreateDialog [savedInstanceState]
      (TimePickerDialog. activity this 0 0 true))
    (onTimeSet [view hourOfDay minute]
      (set-elmt-text activity ::time
        (format "%d"  (+ (* hourOfDay 60) minute))))))

(defactivity com.example.gt.metime.MainActivity
  :key :main

  (onCreate [this bundle]
    (.superOnCreate this bundle)
    (on-ui (set-content-view! (*a) (main-layout (*a))))))