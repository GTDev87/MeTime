(ns com.gt.metime.main
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
           java.text.SimpleDateFormat
           [android.os SystemClock CountDownTimer]
           [java.util Date Calendar TimeZone]
           java.util.concurrent.TimeUnit
           [android.app Activity DialogFragment]
           android.view.View
           [android.app DatePickerDialog DatePickerDialog$OnDateSetListener]
           [android.app TimePickerDialog TimePickerDialog$OnTimeSetListener]))

(declare add-event)
(declare date-picker)
(declare time-picker)

(defn millis-to-format-time [millis]
  (str (format "%02d:%02d:%02d"
               (.toHours TimeUnit/MILLISECONDS millis)
               (- (.toMinutes TimeUnit/MILLISECONDS millis) (.toMinutes TimeUnit/HOURS (.toHours TimeUnit/MILLISECONDS millis)))
               (- (.toSeconds TimeUnit/MILLISECONDS millis) (.toSeconds TimeUnit/MINUTES (.toMinutes TimeUnit/MILLISECONDS millis))))))

(defn priv-count-up-timer
  [text-time-view end-time countdown-timer-interval finish-fn start-time]
  (let [duration (- end-time start-time)]
    (proxy [CountDownTimer] [duration countdown-timer-interval]
      (onTick [millis-until-finished]
        (let [millis-complete (+ start-time (- duration millis-until-finished))]
          (on-ui
            (config
              text-time-view
              :text
              (millis-to-format-time millis-complete)))))
      (onFinish [] (finish-fn)))))

  (defn create-count-up-timer-class
    ([text-time-view millis-in-future countdown-timer-interval finish-fn] (priv-count-up-timer text-time-view millis-in-future countdown-timer-interval finish-fn 0))
    ([text-time-view millis-in-future countdown-timer-interval finish-fn start-time] (priv-count-up-timer text-time-view millis-in-future countdown-timer-interval finish-fn start-time)))

  (defn show-picker [activity picker picker-type]
    (. picker show
       (. activity getFragmentManager)
       picker-type))

  (def listing (atom (sorted-map)))

  (defrecord Task [date name duration date-index])

  (defn add-to-listing [event-name date-key time-key]
    (swap! listing update-in [date-key] (fnil conj []) [event-name time-key]))

  (defn sorted-map-array-to-array-task [lst]
    (mapcat
      identity
      (map
        (fn [sorted-map-data]
          (into [] (map-indexed (fn [i ele] (apply ->Task (concat [(first sorted-map-data)] ele [i]))) (second sorted-map-data))))
        lst)))

  (declare start-timer)
  (declare stop-timer)

  (def simple-date-format (doto (SimpleDateFormat. "HH:mm:ss")
                            (.setTimeZone (TimeZone/getTimeZone "GMT"))))

  (defn format-time-to-millis [event-timer-view] (.getTime (.parse simple-date-format (.getText ^TextView event-timer-view))))

  (defn stop-timer [timer duration event-timer-view event-button-view]
    (config event-button-view :text "Stop")
    (config event-button-view :on-click (fn [_]
                                          (.cancel ^CountDownTimer timer)
                                          (start-timer duration event-timer-view event-button-view (format-time-to-millis event-timer-view)))))

  (defn start-timer [duration event-timer-view event-button-view start-time]
    (let [timer (create-count-up-timer-class
                  event-timer-view
                  (* duration 60000)
                  1000
                  (fn []
                    (on-ui (config event-timer-view :text "Completed."))
                    (start-timer duration event-timer-view event-button-view 0)
                    (on-ui (config event-button-view :text "Reset")))
                  start-time)]
      (config event-button-view :text "Start")
      (config event-button-view :on-click (fn [_] (.start ^CountDownTimer timer)
                                            (stop-timer timer duration event-timer-view event-button-view)))))

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
          [:text-view {:id ::time-tv}]
          [:button {:id ::event-btn}]]])
      (fn [_ view _ task]
        (let [date-text-view (find-view view ::date-tv)
              event-linear-layout-view (find-view view ::event-ll)
              event-text-view (find-view event-linear-layout-view ::event-tv)
              event-timer-view (find-view event-linear-layout-view ::time-tv)
              event-button-view (find-view event-linear-layout-view ::event-btn)]

          ;mutates the viz
          (config date-text-view :visibility (if (= (:date-index task) 0) View/VISIBLE View/GONE))
          (config date-text-view :text (str (:date task)))

          (start-timer (:duration task) event-timer-view event-button-view 0)

          (config event-text-view :text (str (:name task) " (Goal: " ( millis-to-format-time (* 1000 60 (:duration task))) ") "))))
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