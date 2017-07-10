(ns trader.utils
  (:require [clj-http.util :refer [url-encode]]
            [clojure.core.async :as a]
            [clojure.string :as string]))

(defn format-float [rate]
  (format "%.8f" rate))

(defn truncate-float [rate]
  (Float/parseFloat (format-float rate)))

(defn every
  ([schedule f]
   (every 0 schedule f))
  ([delay schedule f]
   (let [cancel (a/chan)]
     (a/thread
       (loop [timer (a/timeout delay)]
         (when (a/alt!!
                 cancel false
                 timer true)
           (let [start (System/currentTimeMillis)
                 _ (f)
                 end (System/currentTimeMillis)
                 next-timeout (a/timeout (- schedule (- end start)))]
             (recur next-timeout)))))
     cancel)))

(defn cancel-scheduler [chan]
  (a/>!! chan :cancel))

(defn url-encode-map [m]
  (->> m
       (map (fn [[k v]]
              (format "%s=%s" (url-encode (name k)) (url-encode (str v)))))
       (string/join "&")))
