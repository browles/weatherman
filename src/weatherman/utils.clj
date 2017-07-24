(ns weatherman.utils
  (:require [clj-http.util :refer [url-encode]]
            [clojure.core.async :as a]
            [clojure.string :as string]
            [clojure.tools.logging :as log]))

;; core.async utils
(defmacro safe-thread [& body]
  `(a/thread
     (try
       ~@body
       (catch Exception e#
         (log/fatal "Uncaught exception in background thread!" e#)
         (System/exit 1)))))

(defmacro safe-go [& body]
  `(a/go
     (try
       ~@body
       (catch Exception e#
         (log/fatal "Uncaught exception in background go routine!" e#)
         (System/exit 1)))))

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

(defn throttle [f limit period]
  (let [throttle-chan (a/chan limit)]
    (dotimes [_ limit] (a/>!! throttle-chan (a/timeout 0)))
    (fn [& args]
      (a/<!! (a/<!! throttle-chan))
      (a/put! throttle-chan (a/timeout period))
      (apply f args))))

(defn repeatedly-chan [f buf]
  (let [out (a/chan buf)]
    (safe-thread
      (while true
        (a/>!! out (f))))
    out))

(defn to-seq [c]
  (when-some [head (a/<!! c)]
    (lazy-seq (cons head (to-seq c)))))

;; misc
(defn format-float [rate]
  (format "%.8f" (- rate 0.000000005)))

(defn truncate-float [rate]
  (Double/parseDouble (format-float rate)))

(defn diff-map [a b & exclusions]
  (let [diff (->> b
                  (filter (fn [[k v]]
                            (not= (k a) v)))
                  (into {}))]
    (reduce #(assoc %1 %2 (%2 b)) diff exclusions)))

(defn pairs [coll]
  (->> (map list coll coll)
       flatten
       (drop 1)
       (drop-last 1)
       (partition-all 2)))

(defn rotate-until [item coll]
  (->> coll
       cycle
       (drop-while #(not= % item))
       (take (count coll))))

(defn url-encode-map [m]
  (->> m
       (map (fn [[k v]]
              (when-not (nil? v)
                (format "%s=%s" (url-encode (name k)) (url-encode (str v))))))
       (keep identity)
       (string/join "&")))

(defn assert-contains
  [set-atom message item & {:as opts}]
  (if opts
    (assert (or (= (:allow opts) item)
                (contains? @set-atom item))
            (format message item))
    (assert (contains? @set-atom item)
            (format message item))))
