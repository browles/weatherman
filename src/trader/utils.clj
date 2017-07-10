(ns trader.utils
  (:require [clojure.core.async :as a]))

(defn every [schedule f & args]
  (let [cancel (a/chan)]
    (a/thread
      (loop [run true]
        (when run
          (let [start (System/currentTimeMillis)
                _ (apply f args)
                end (System/currentTimeMillis)
                timer (a/timeout (- schedule (- end start)))]
            (recur (a/alt!!
                     cancel false
                     timer true))))))
    cancel))
