(ns weatherman.utils
  (:require [clj-http.util :refer [url-encode]]
            [clojure.core.async :as a]
            [clojure.string :as string]))

(defmacro safe-thread [& body]
  `(a/thread
     (try
       ~@body
       (catch Exception e#
         (prn "Uncaught exception in background thread!")
         (prn e#)
         (System/exit 1)))))

(defn format-float [rate]
  (format "%.8f" rate))

(defn truncate-float [rate]
  (Float/parseFloat (format-float rate)))

(defn every
  ([schedule f]
   (every 0 schedule f))
  ([delay schedule f]
   (let [cancel (a/chan)]
     (safe-thread
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

(defn throttle [f limit regen]
  (let [throttle-chan (a/chan (a/dropping-buffer limit))]
    (dotimes [_ limit] (a/>!! throttle-chan :tick))
    (fn [& args]
      (a/<!! throttle-chan)
      (a/go (a/<! (a/timeout regen))
            (a/>! throttle-chan :tick))
      (apply f args))))

(defn cancel [chan]
  (a/>!! chan :cancel))

(defn url-encode-map [m]
  (->> m
       (map (fn [[k v]]
              (when-not (nil? v)
                (format "%s=%s" (url-encode (name k)) (url-encode (str v))))))
       (keep identity)
       (string/join "&")))

(defn- assert-contains
  [set-atom message item & {:as opts}]
  (assert @set-atom "set-atom is nil!")
  (if opts
    (assert (or (= (:allow opts) item)
                (contains? @set-atom item))
            (format message item))
    (assert (contains? @set-atom item)
            (format message item))))
