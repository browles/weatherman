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

(def start-time (c/to-long "2017-07-23T17:00:59.426"))
(def end-time (c/to-long "2017-07-30T00:00:00.000"))

(def fee 0.0015)
(def fee-ratio (bigdec (- 1 fee)))

(defn get-ticker-ingestions []
  (db/query ["SELECT * FROM ticker_ingestions WHERE api_end > ? AND api_end < ? ORDER BY api_end ASC" start-time end-time]))

(defn get-tickers [{:keys [id] :as ingestion}]
  {:ingestion ingestion
   :tickers (db/query ["SELECT * FROM ticker WHERE ingestion_id = ?" id])})

(defn to-graph-patch [{:keys [ingestion tickers]}]
  {:ingestion ingestion
   :graph (reduce (fn [patch {:keys [currency_pair last highest_bid lowest_ask]}]
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
                  tickers)})

(defn apply-patch [state patch]
  {:ingestion (:ingestion patch)
   :graph (merge-with merge (:graph state) (:graph patch))})

(defn bellman-ford [graph source]
  (let [node (fn [_] (hash-map :d Double/POSITIVE_INFINITY :p nil))
        V (atom (pc/map-vals node graph))
        E (->> graph
               (map (fn [[k v]] (map #(vector k %) (keys v))))
               (apply concat))
        cycles (atom #{})
        weight (fn [u v] (bigdec (get-in graph [u v :weight])))
        get-node (fn [v] (get @V v))
        relax (fn [u v]
                (let [u-node (get-node u)
                      v-node (get-node v)
                      w (+ (:d u-node) (weight u v))]
                  (when (> (:d v-node) w)
                    (swap! V #(assoc % v {:d w :p u})))))]
    (swap! V #(assoc % source {:d 0M :p nil}))
    (dotimes [i (- (count graph) 1)]
      (doseq [[u v] E]
        (relax u v)))
    (doseq [[u v] E]
      (when (and (> (:d (get-node v)) (+ (:d (get-node u)) (weight u v))))
        (loop [curr v path [] seen #{}]
          (if (seen curr)
            (swap! cycles (fn [cycles-vec]
                            (let [cyc (-> (drop-while #(not= % curr) path)
                                          reverse)
                                  start (first (sort cyc))
                                  capped-cycle (-> (utils/rotate-until start cyc)
                                                   vec
                                                   (conj start))]
                              (conj cycles-vec capped-cycle))))
            (recur (:p (get-node curr)) (conj path curr) (conj seen curr))))))
    @cycles))

(defn validate-cycle [graph path]
  (when (and (> (count path) 3)
             (< (count path) 10))
    (let [actual (->> (utils/pairs path)
                      (map #(get-in graph %))
                      (map (comp bigdec :last))
                      (map #(* % fee-ratio))
                      (reduce *))]
      (when (> actual 1)
        actual))))

(def report (atom []))

(defn process []
  (reset! report [])
  (let [ticker-state (atom {})
        tickers (->> (get-ticker-ingestions)
                     (map get-tickers)
                     (map to-graph-patch)
                     (map (fn [p] (swap! ticker-state #(apply-patch % p)))))]
    (dorun (utils/chunked-pmap (fn [{:keys [ingestion graph]}]
                                 (let [cycles (bellman-ford graph :BTC)
                                       valid-cycle? (memoize (partial validate-cycle graph))
                                       valid (filter valid-cycle? cycles)
                                       values (keep valid-cycle? cycles)
                                       reports (map #(hash-map :cycle %1 :value %2 :ingestion ingestion) valid values)]
                                   (swap! report #(into [] (concat % reports)))))
                               512
                               tickers))))
