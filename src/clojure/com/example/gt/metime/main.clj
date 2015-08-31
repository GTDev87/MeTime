(ns com.example.gt.metime.main
  (:require [neko.activity :refer [defactivity set-content-view!]]
            [neko.debug :refer [*a]]
            [neko.ui :refer [config]]
            [neko.log :as log]
            [clojure.string :refer [join]]
            [neko.find-view :refer [find-view]]
            [neko.threading :refer [on-ui]])
  (:import android.widget.TextView
           (java.util Calendar)
           (android.app Activity)
           (android.app DatePickerDialog DatePickerDialog$OnDateSetListener)
           (android.app TimePickerDialog TimePickerDialog$OnTimeSetListener)
           (android.app DialogFragment)))

(declare add-event)
(declare date-picker)
(declare time-picker)

(defn show-picker [activity dp picker-type]
  (. dp show (. activity getFragmentManager) picker-type))

(def listing (atom (sorted-map)))

(defn format-events [events]
  (->> (map (fn [[event]]
    (format "%s\n" event))
    events)
  (join "                      ")))

(defn format-listing [lst]
  (->> (map (fn [[date events]]
      (format "%s - %s" date (format-events events)))
    lst)
  join))

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
    [:text-view {
      :text (format-listing @listing)
      :id ::listing}]])

(defn get-elmt [activity elmt]
  (str (.getText ^TextView (find-view activity elmt))))

(defn set-elmt [activity elmt s]
  (on-ui (config (find-view activity elmt) :text s)))

(defn update-ui [activity]
  (set-elmt activity ::listing (format-listing @listing))
  (set-elmt activity ::name ""))

(defn add-event [activity]
  (let [date-key (try
      (read-string (get-elmt activity ::date))
      (catch RuntimeException e "Date string is empty!"))]
    (when (number? date-key)
      (swap! listing update-in [date-key] (fnil conj [])
        [(get-elmt activity ::name)])
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
      (set-elmt activity ::date
        (format "%d%02d%02d" year (inc month) day)))))

(defn time-picker [activity]
  (proxy [DialogFragment TimePickerDialog$OnTimeSetListener] []
    (onCreateDialog [savedInstanceState]
      (TimePickerDialog. activity this 0 0 true))
    (onTimeSet [view hourOfDay minute]
      (set-elmt activity ::time
        (format "%02d:%02d" hourOfDay minute)))))

(defactivity com.example.gt.metime.MainActivity
  :key :main

  (onCreate [this bundle]
    (.superOnCreate this bundle)
    (on-ui
      (set-content-view! (*a) (main-layout (*a)))
      (set-elmt (*a) ::listing (format-listing @listing)))
    ))