(ns com.gt.metime.main
  (:require [neko.activity :refer [defactivity set-content-view!]]
            [neko.debug :refer [*a]]
            [neko.ui :refer [config make-ui]]
            [neko.ui.mapping :refer [defelement]]
            [neko.log :as log]
            [neko.ui.adapters :as adapters]
            [clojure.string :refer [join]]
            [neko.find-view :refer [find-view]]
            [neko.threading :refer [on-ui]]
            [com.gt.metime.time :refer [millis-to-format-time format-time-to-millis]]
            [com.gt.metime.listing :refer [listing add-to-listing remove-from-listing sorted-map-array-to-array-task]])
  (:import [android.os SystemClock CountDownTimer]
           [android.widget CursorAdapter TextView LinearLayout Chronometer]
           android.graphics.Color
           [java.util Calendar]
           [android.app Activity DialogFragment]
           android.view.View
           [android.app DatePickerDialog DatePickerDialog$OnDateSetListener]
           [android.app TimePickerDialog TimePickerDialog$OnTimeSetListener]))

(declare add-event)
(declare date-picker)
(declare time-picker)

(defn show-picker [activity picker picker-type]
  (. picker show (. activity getFragmentManager) picker-type))

(declare start-timer-set)
(declare stop-timer-set)

(defrecord TimerContext [running offset base])

(defn stop-timer-set [event-chonometer event-button-view timer-context]
  (config event-button-view :text "Stop")
  (config event-button-view :on-click (fn [_]
                                        (.stop event-chonometer)
                                        (reset! timer-context (apply ->TimerContext [false (- (.getBase event-chonometer) (SystemClock/elapsedRealtime)) (.getBase event-chonometer)]))
                                        (start-timer-set event-chonometer event-button-view timer-context))))

(defn start-timer-set [event-chonometer event-button-view timer-context]
  (config event-button-view :text "Start")
  (config event-button-view :on-click (fn [_]
                                        (reset! timer-context (apply ->TimerContext [true (:offset @timer-context) (+ (SystemClock/elapsedRealtime) (:offset @timer-context))]))
                                        (.setBase event-chonometer (:base @timer-context))
                                        (.start event-chonometer)
                                        (stop-timer-set event-chonometer event-button-view timer-context))))

(defelement :chronometer
            :classname android.widget.Chronometer
            :inherits  :text-view)

(defn make-date-adapter []
  (adapters/ref-adapter
    (fn [_]
      [:linear-layout {:id-holder   true
                       :orientation :vertical}
       [:text-view {:id ::date-tv}]
       [:linear-layout {:id          ::event-ll
                        :id-holder   true
                        :orientation :horizontal}
        [:text-view {:id ::event-tv}]
        [:text-view {:id ::goal-tv}]
        [:chronometer {:id ::time-tv}]
        [:button {:id ::event-btn}]
        [:button {:id   ::delete-btn
                  :text "del"}]]])
    (fn [indx view _ task]
      (let [date-text-view (find-view view ::date-tv)
            event-linear-layout-view (find-view view ::event-ll)
            event-text-view (find-view event-linear-layout-view ::event-tv)
            event-chonometer (find-view event-linear-layout-view ::time-tv)
            event-button-view (find-view event-linear-layout-view ::event-btn)
            event-delete-button-view (find-view event-linear-layout-view ::delete-btn)
            timer-context (:timer-context task)]

        ;mutates the viz
        (config date-text-view :visibility (if (= (:date-index task) 0) View/VISIBLE View/GONE))
        (config date-text-view :text (str (:date task)))

        (.stop event-chonometer)
        (config event-chonometer :text "00:00")

        (if
          (:running @timer-context)
          ((fn []
             (.setBase event-chonometer (+ (:base @timer-context) (:offset @timer-context)))
             (reset! timer-context (apply ->TimerContext [true (:offset @timer-context) (:base @timer-context)]))
             (.start event-chonometer)
             (config event-button-view :text "Stop")
             (stop-timer-set event-chonometer event-button-view timer-context)))
          ((fn []
             (config event-button-view :text "Start")
             (start-timer-set event-chonometer event-button-view timer-context))))

        (config event-text-view :text (str (:name task) " (Goal: " (millis-to-format-time (* 1000 60 (:duration task))) ") "))

        (config event-delete-button-view :on-click (fn [_]
                                                     (.stop event-chonometer)
                                                     (reset! timer-context (apply ->TimerContext [false (- (.getBase event-chonometer) (SystemClock/elapsedRealtime)) (.getBase event-chonometer)]))
                                                     (remove-from-listing (:date task) (:date-index task))))))
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
    [:button {:text     "Set Time...",
              :on-click (fn [_] (show-picker activity
                                             (time-picker activity), "timePicker"))}]]
   [:linear-layout {:orientation :horizontal}
    [:text-view {:hint "Event date",
                 :id   ::date}]
    [:button {:text     "Set Date...",
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

(defactivity com.gt.metime.MainActivity
             :key :main
             (onCreate [this bundle]
                       (.superOnCreate this bundle)
                       (on-ui (set-content-view! (*a) (main-layout (*a))))))