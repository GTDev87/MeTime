(ns com.gt.metime.timer
  (:import [android.os CountDownTimer]))

(defn priv-count-up-timer
  [end-time countdown-timer-interval count-fn finish-fn start-time]
  (let [duration (- end-time start-time)]
    (proxy [CountDownTimer] [duration countdown-timer-interval]
      (onTick [millis-until-finished]
        (count-fn (+ start-time (- duration millis-until-finished))))
      (onFinish [] (finish-fn)))))

(defn create-count-up-timer-class
  ([millis-in-future countdown-timer-interval count-fn finish-fn] (priv-count-up-timer millis-in-future countdown-timer-interval count-fn finish-fn 0))
  ([millis-in-future countdown-timer-interval count-fn finish-fn start-time] (priv-count-up-timer millis-in-future countdown-timer-interval count-fn finish-fn start-time)))
