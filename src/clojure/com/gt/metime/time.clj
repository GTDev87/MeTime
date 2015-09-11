(ns com.gt.metime.time
  (:import java.text.SimpleDateFormat
           [java.util Date TimeZone]
           java.util.concurrent.TimeUnit
           java.util.Locale))

;time
(def simple-time-format (doto (SimpleDateFormat. "HH:mm:ss")
                          (.setTimeZone (TimeZone/getTimeZone "GMT"))))

(defn millis-to-format-time [millis]
  (str (format "%02d:%02d:%02d"
               (.toHours TimeUnit/MILLISECONDS millis)
               (- (.toMinutes TimeUnit/MILLISECONDS millis) (.toMinutes TimeUnit/HOURS (.toHours TimeUnit/MILLISECONDS millis)))
               (- (.toSeconds TimeUnit/MILLISECONDS millis) (.toSeconds TimeUnit/MINUTES (.toMinutes TimeUnit/MILLISECONDS millis))))))

(defn format-time-to-millis [time-str] (.getTime (.parse simple-time-format time-str)))

;end time

;date
(def stored-date "yyyyMMdd")
(def readable-date "MM/dd/yyyy")

(defn format-converter-creator [old-format new-format]
  (fn [old-format-string]
    (let [ sdf-old (doto (SimpleDateFormat. old-format (. Locale US))
                     (.setTimeZone (TimeZone/getTimeZone "GMT")))
          date (.parse sdf-old old-format-string)]
      (.format
        (doto
          sdf-old
          (.applyPattern new-format))
        date))))

(def stored-to-readable-date (format-converter-creator stored-date readable-date))
(def readable-to-stored-date (format-converter-creator readable-date stored-date))
;end date