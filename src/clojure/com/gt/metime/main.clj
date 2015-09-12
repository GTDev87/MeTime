(ns com.gt.metime.main
  (:require [neko.activity :refer [defactivity set-content-view!]]
            [neko.debug :refer [*a]]
            [neko.ui :refer [config make-ui]]
            [neko.ui.mapping :refer [defelement]]
            [neko.dialog.alert :refer [alert-dialog-builder]]
            [neko.log :as log]
            [neko.ui.adapters :as adapters]
            [clojure.string :refer [join]]
            [neko.find-view :refer [find-view]]
            [neko.threading :refer [on-ui]]
            [com.gt.metime.time :refer [millis-to-format-time format-time-to-millis stored-to-readable-date readable-to-stored-date]]
            [com.gt.metime.listing :refer [listing add-to-listing remove-from-listing sorted-map-array-to-array-task update-me-time-offset!]])
  (:import [android.os SystemClock CountDownTimer]
           [android.widget CursorAdapter TextView LinearLayout Chronometer Chronometer$OnChronometerTickListener]
           [java.util Calendar]
           android.content.DialogInterface
           [android.app Activity DialogFragment]
           android.view.View
           [android.app DatePickerDialog DatePickerDialog$OnDateSetListener]
           [android.app TimePickerDialog TimePickerDialog$OnTimeSetListener]))

(declare add-event)
(declare date-picker)
(declare time-picker)
(declare alert-dialog)

(defn show-picker [activity picker picker-type]
  (. picker show (. activity getFragmentManager) picker-type))

(declare start-timer)
(declare stop-timer)

(defrecord TimerContext [running offset base])

(defn start-timer [event-chonometer event-button-view timer-context]
  (.stop event-chonometer)
  (config event-button-view :text "Start")
  (reset! timer-context (apply ->TimerContext [false (- (.getBase event-chonometer) (SystemClock/elapsedRealtime)) (.getBase event-chonometer)]))
  (config event-button-view :on-click (fn [_] (stop-timer event-chonometer event-button-view timer-context))))

(defn stop-timer [event-chonometer event-button-view timer-context]
  (reset! timer-context (apply ->TimerContext [true (:offset @timer-context)  (+ (SystemClock/elapsedRealtime) (:offset @timer-context))]))
  (.setBase event-chonometer (:base @timer-context))
  (config event-button-view :text "Stop")
  (.start event-chonometer)
  (config event-button-view :on-click (fn [_] (start-timer event-chonometer event-button-view timer-context))))

(defelement :chronometer
            :classname android.widget.Chronometer
            :inherits :text-view)

(defn get-chronometer-listener []
  (proxy [Chronometer$OnChronometerTickListener] []
    (onChronometerTick [chronometer]
      (config chronometer :text (str (millis-to-format-time (- (SystemClock/elapsedRealtime) (.getBase chronometer))))))))

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
    (fn [_ view _ task]
      (let [date-text-view (find-view view ::date-tv)
            event-linear-layout-view (find-view view ::event-ll)
            event-text-view (find-view event-linear-layout-view ::event-tv)
            event-chonometer (find-view event-linear-layout-view ::time-tv)
            event-button-view (find-view event-linear-layout-view ::event-btn)
            event-delete-button-view (find-view event-linear-layout-view ::delete-btn)
            timer-context (:timer-context task)]

        (.setOnChronometerTickListener event-chonometer (get-chronometer-listener))

        ;mutates the viz
        (config date-text-view :visibility (if (= (:date-index task) 0) View/VISIBLE View/GONE))
        (config date-text-view :text (stored-to-readable-date (str (:date task))))

        (.stop event-chonometer)

        (if
          (:running @timer-context)
          ((fn []
             (reset! timer-context (apply ->TimerContext [true (:offset @timer-context) (:base @timer-context)]))
             (.setBase event-chonometer (:base @timer-context))
             (.start event-chonometer)
             (config event-button-view :text "Stop")

             (config event-button-view :on-click (fn [_] (start-timer event-chonometer event-button-view timer-context)))
             ))
          ((fn []
             (config event-chonometer :text (millis-to-format-time (* (:offset @timer-context) -1)))
             (config event-button-view :text "Start")

             (config event-button-view :on-click (fn [_] (stop-timer event-chonometer event-button-view timer-context)))
             )))

        (config event-text-view :text (str (:name task) " (Goal: " (millis-to-format-time (* 1000 60 (:duration task))) ") "))

        (config event-delete-button-view :on-click (fn [_]
                                                     (.stop event-chonometer)
                                                     (let [added-time (if (:running @timer-context) (- (.getBase event-chonometer) (SystemClock/elapsedRealtime)) (:offset @timer-context) )]
                                                       (reset! timer-context (apply ->TimerContext [false (- (.getBase event-chonometer) (SystemClock/elapsedRealtime)) (.getBase event-chonometer)]))
                                                       (update-me-time-offset! (get @listing (:date task)) added-time)
                                                       (remove-from-listing (:date task) (:date-index task)))))))
    listing
    sorted-map-array-to-array-task))

(defn main-layout [activity]
  [:linear-layout {:orientation :vertical}
   [:button {:text     "+ Task",
             :on-click (fn [_]
                         (show-picker
                           activity
                           (alert-dialog
                             activity
                             {:message           "Add Task"
                              :cancelable        true
                              :positive-text     "OK"
                              :positive-callback (fn [_ _]) ;will be overwitten below... ugh mutable
                              :negative-text     "Cancel"
                              :negative-callback (fn [_ _])}
                             (fn []
                               [:linear-layout {:id-holder   true
                                                :id          ::top-dialog-layout
                                                :orientation :vertical}
                                [:edit-text {:hint "Task name"
                                             :id   ::name}]
                                [:linear-layout {:orientation :horizontal
                                                 :id          ::ll}
                                 [:text-view {:hint "Goal (Time)"
                                              :id   ::time}]
                                 [:button {:text "Set Goal..."
                                           :id   ::time-btn}]]
                                [:linear-layout {:orientation :horizontal}
                                 [:text-view {:hint "Event date",
                                              :id   ::date}]
                                 [:button {:text "Set Date..."
                                           :id   ::date-btn}]
                                 ]])
                             (fn [dialog alert-dialog-view]
                               (let [time-button-view (find-view alert-dialog-view ::time-btn)
                                     date-button-view (find-view alert-dialog-view ::date-btn)
                                     name-text-view (find-view alert-dialog-view ::name)
                                     time-text-view (find-view alert-dialog-view ::time)
                                     date-text-view (find-view alert-dialog-view ::date)
                                     dialog-confirm-button (.getButton dialog (. DialogInterface BUTTON_POSITIVE))]

                                 (config time-button-view :on-click (fn [_] (show-picker activity (time-picker activity time-text-view) "timePicker")))
                                 (config date-button-view :on-click (fn [_] (show-picker activity (date-picker activity date-text-view) "datePicker")))

                                 (config dialog-confirm-button :on-click (fn [_]
                                                                           (add-event date-text-view time-text-view name-text-view)
                                                                           (.dismiss dialog))))))
                           "alertDialog"))}]
   [:list-view {:id                   ::days
                :draw-selector-on-top true
                :adapter              (make-date-adapter)}]])

(defn set-elmt-text [view s] (on-ui (config view :text s)))

(defn add-event [date-view time-view name-view]
  (let [
        date-key (try
                   (read-string  (readable-to-stored-date (.getText ^TextView date-view)))
                   (catch RuntimeException e "Date string is empty!"))
        time-key (try
                   (read-string (.getText ^TextView time-view))
                   (catch RuntimeException e "Time string is empty!"))]
    (when (number? date-key)
      (add-to-listing (.getText ^TextView name-view) date-key time-key))))

(defn date-picker [activity view]
  (proxy [DialogFragment DatePickerDialog$OnDateSetListener] []
    (onCreateDialog [_]
      (let [c (Calendar/getInstance)
            year (.get c Calendar/YEAR)
            month (.get c Calendar/MONTH)
            day (.get c Calendar/DAY_OF_MONTH)]
        (DatePickerDialog. activity this year month day)))
    (onDateSet [_ year month day]
      (set-elmt-text view (stored-to-readable-date (format "%d%02d%02d" year (inc month) day))))))

(defn alert-dialog [activity options view-fn update-view-fn]
  (proxy [DialogFragment] []
    (onCreateDialog [_]
      (let [alert-dialog-view (make-ui activity (view-fn))]
        (doto
          (-> (alert-dialog-builder activity options) .create)
          (.setView (make-ui activity alert-dialog-view))
          (.show)
          (update-view-fn alert-dialog-view))))));needs to run after set

(defn time-picker [activity view]
  (proxy [DialogFragment TimePickerDialog$OnTimeSetListener] []
    (onCreateDialog [_]
      (TimePickerDialog. activity this 0 0 true))
    (onTimeSet [_ hourOfDay minute]
      (set-elmt-text view (format "%d" (+ (* hourOfDay 60) minute))))))

(defactivity com.gt.metime.MainActivity
             :key :main
             (onCreate [this bundle]
                       (.superOnCreate this bundle)
                       (on-ui (set-content-view! (*a) (main-layout (*a))))))