(ns com.gt.metime.time
  (:import java.text.SimpleDateFormat
           [java.util Date TimeZone]
           java.util.concurrent.TimeUnit))

(def simple-date-format (doto (SimpleDateFormat. "HH:mm:ss")
                          (.setTimeZone (TimeZone/getTimeZone "GMT"))))

(defn millis-to-format-time [millis]
  (str (format "%02d:%02d:%02d"
               (.toHours TimeUnit/MILLISECONDS millis)
               (- (.toMinutes TimeUnit/MILLISECONDS millis) (.toMinutes TimeUnit/HOURS (.toHours TimeUnit/MILLISECONDS millis)))
               (- (.toSeconds TimeUnit/MILLISECONDS millis) (.toSeconds TimeUnit/MINUTES (.toMinutes TimeUnit/MILLISECONDS millis))))))

(defn format-time-to-millis [time-str] (.getTime (.parse simple-date-format time-str)))