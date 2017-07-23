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

(defn group-patch [ingestion-tickers]
  (reduce (fn [patch {:keys [currency_pair last highest_bid lowest_ask]}]
            (let [[a b] (->> (string/split currency_pair #"_") (map keyword))]
              (assoc patch
                a {b {:last last
                      :highest-bid highest_bid
                      :lowest-ask lowest_ask}}
                b {a {:last (/ 1 last)
                      :highest-bid (/ 1 highest_bid)
                      :lowest-ask (/ 1 lowest_ask)}})))
          {:ingestion (:ingestion ingestion-tickers)}
          (:tickers ingestion-tickers)))

(defn apply-patch [patch state]
  (merge-with merge state patch))

(defn get-weights [patch]
  (pc/map-vals (fn [c]
                 (pc/map-vals (fn [t]
                                (- (Math/log (:last t))))
                              c))
               patch))

(defn process []
  (let [all-ticker (atom {})
        graph (atom {})
        ticker-patches (->> (get-ticker-ingestions)
                            (map get-tickers)
                            (map group-patch))]
    (doseq [patch ticker-patches]
      (swap! all-ticker apply-patch patch)
      (swap! graph apply-patch (get-weights patch)))))
