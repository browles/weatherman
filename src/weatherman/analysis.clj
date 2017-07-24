(ns weatherman.analysis
  (:require [clj-time.coerce :as c]
            [clojure.core.async :as a]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [plumbing.core :as pc]
            [plumbing.map :as pm]
            [weatherman.api :as api]
            [weatherman.db :as db]
            [weatherman.utils :as utils]))

(set! *warn-on-reflection* true)

(def start-time (c/to-long "2017-07-18T19:43:33.725"))
(def end-time (c/to-long "2017-07-20T22:28:36.112"))

(defn get-ticker-ingestions []
  (db/query ["SELECT * FROM ticker_ingestions WHERE api_end > ? AND api_end < ? ORDER BY api_end ASC" start-time end-time]))

(defn get-tickers [{:keys [id] :as ingestion}]
  {:ingestion ingestion
   :tickers (db/query ["SELECT * FROM ticker WHERE ingestion_id = ?" id])})

(defn group-patch [{:keys [ingestion tickers]}]
  (reduce (fn [patch {:keys [currency_pair last highest_bid lowest_ask]}]
            (let [[a b] (->> (string/split currency_pair #"_") (map keyword))
                  weight (- (Math/log last))]
              (assoc patch
                a {b {:weight weight
                      :last last
                      :highest-bid highest_bid
                      :lowest-ask lowest_ask}}
                b {a {:weight (- weight)
                      :last (/ 1 last)
                      :highest-bid (/ 1 highest_bid)
                      :lowest-ask (/ 1 lowest_ask)}})))
          {}
          tickers))

(defn apply-patch [state patch]
  (merge-with merge state patch))

(defn bellman-ford [graph source]
  (let [node (fn [_] (hash-map :d Double/POSITIVE_INFINITY :p nil))
        V (atom (pc/map-vals node graph))
        E (->> graph
               (map (fn [[k v]] (map #(vector k %) (keys v))))
               (apply concat))
        cycles (atom #{})
        weight (fn [u v] (get-in graph [u v :weight]))
        get-node (fn [v] (get @V v))
        relax (fn [u v]
                (let [u-node (get-node u)
                      v-node (get-node v)
                      w (+ (:d u-node) (weight u v))]
                  (when (> (:d v-node) w)
                    (swap! V #(assoc % v {:d w :p u})))))]
    (swap! V #(assoc % source {:d 0 :p nil}))
    (dotimes [i (- (count graph) 1)]
      (doseq [[u v] E]
        (relax u v)))
    (doseq [[u v] E]
      (when (and (> (:d (get-node v)) (+ (:d (get-node u)) (weight u v))))
        (loop [curr v path [] seen #{}]
          (if (seen curr)
            (swap! cycles (fn [state]
                            (conj state
                                  (->> path
                                       (drop-while #(not= % curr))
                                       reverse
                                       (#(utils/rotate-until (first (sort %)) %))))))
            (recur (:p (get-node curr)) (conj path curr) (conj seen curr))))))
    @cycles))

(defn validate-cycle [graph path]
  (when (and (> (count path) 2)
             (< (count path) 10))
    (let [actual (->> path
                      cycle
                      (take (inc (count path)))
                      utils/pairs
                      (map #(get-in graph %))
                      (map :last)
                      (reduce *))]
      (when (and actual (> actual 1))
        actual))))

(def hist (atom {}))

(defn process []
  (reset! hist {})
  (let [graph (atom {})
        tickers (->> (get-ticker-ingestions)
                     (map get-tickers)
                     (map group-patch)
                     (map (fn [p] (swap! graph #(apply-patch % p)))))]
    (dorun (pmap (fn [ticker]
                   (let [cycles (bellman-ford ticker :BTC)
                         pred (memoize (partial validate-cycle ticker))
                         valid (filter pred cycles)
                         val (keep pred cycles)]
                     (doseq [c valid]
                       (swap! hist #(update % c (fnil inc 0))))))
                 tickers))
    @graph
    nil))
