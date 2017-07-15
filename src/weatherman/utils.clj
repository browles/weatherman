(ns weatherman.utils
  (:require [clj-http.util :refer [url-encode]]
            [clojure.core.async :as a]
            [clojure.string :as string]))

;; core.async utils
(defmacro safe-thread [& body]
  `(a/thread
     (try
       ~@body
       (catch Exception e#
         (prn "Uncaught exception in background thread!")
         (prn e#)
         (System/exit 1)))))

(defmacro safe-go [& body]
  `(a/go
     (try
       ~@body
       (catch Exception e#
         (prn "Uncaught exception in background thread!")
         (prn e#)
         (System/exit 1)))))

(defn every
  ([schedule f]
   (every 0 schedule f))
  ([delay schedule f]
   (let [cancel (a/chan)]
     (safe-go
       (loop [timer (a/timeout delay)]
         (when (a/alt!
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
    (dotimes [_ limit] (a/>!! throttle-chan :tick))
    (fn [& args]
      (a/<!! throttle-chan)
      (a/go (a/<! (a/timeout period))
            (a/>! throttle-chan :tick))
      (apply f args))))

(defn repeatedly-chan
  ([f buf]
   (let [out (a/chan buf)]
     (safe-go
       (while true
         (a/<! out (f))))
     out)))

(defn to-seq [c]
  (when-let [head (a/<!! c)]
    (lazy-seq (cons head (to-seq c)))))

(defn eager-lazy [f buf]
  (to-seq (repeatedly-chanf buf)))

;; misc
(defn format-float [rate]
  (format "%.8f" (- rate 0.000000005)))

(defn truncate-float [rate]
  (Double/parseDouble (format-float rate)))

(defn diff-map [a b]
  (->> b
       (filter (fn [[k v]]
                 (not= (k a) v)))
       (into {})))

(defn pairs [coll]
  (->> (map list coll coll)
       flatten
       (drop 1)
       (drop-last 1)
       (partition-all 2)))

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
