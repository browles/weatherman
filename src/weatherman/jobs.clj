(ns weatherman.jobs
  (:require [clojure.core.async :as a]
            [clojure.tools.logging :as log]
            [mount.core :refer [defstate]]
            [plumbing.core :as pc]
            [plumbing.map :as pm]
            [weatherman.api :as api]
            [weatherman.db :as db]
            [weatherman.utils :as utils])
  (:gen-class))

(defn process-current-market-loan-offers [currency]
  (log/info "Fetching current loan offers for" currency)
  (->> (api/return-loan-orders currency)
       :offers
       (db/record-market-loan-offers)))

(defn choose-rate []
  (let [last-ingestion (db/get-last-row :market_loan_offer_ingestions)
        last-offers (db/select :market_loan_offers {:ingestion_id (:id last-ingestion)})
        bottom-rates (->> last-offers
                          (sort-by :rate <)
                          (drop 1)
                          (take 20)
                          (map :rate))]
    (if (zero? (count bottom-rates))
      0.05
      (/ (reduce + bottom-rates)
         (count bottom-rates)))))

(defn create-loan [currency]
  (log/info "Fetching lending account balance for" currency)
  (let [balance (-> (api/return-available-account-balances "lending")
                    (get-in [:lending (keyword currency)] "0.00")
                    (#(Double/parseDouble %)))
        threshold 0.01
        amount (utils/truncate-float (min balance 0.5))
        rate (utils/truncate-float (choose-rate))
        duration (condp > rate
                   0.02 2
                   60)]
    (when (>= amount threshold)
      (log/info "Attempting to create offer for" amount currency "at" (utils/format-float rate))
      (let [result (api/create-loan-offer "BTC"
                                          amount
                                          duration
                                          0
                                          rate)]
        (if (zero? (:success result))
          (log/info "Failed creating offer for reason:" result)
          (do
            (log/info "Created offer for" amount "at" (utils/format-float rate))
            (db/insert-loan-offer {:order_id (:orderID result)
                                   :amount amount
                                   :duration duration
                                   :rate rate})))))))

(defn loan-control [currency lend?]
  (process-current-market-loan-offers currency)
  (when lend? (create-loan currency)))

(defn poll-ticker [period]
  (let [get-ticker (utils/throttle api/return-ticker 1 period)
        out (utils/repeatedly-chan get-ticker 5)]
    (log/info "Starting to poll for ticker updates.")
    (utils/safe-thread
      (let [parse-price-info (fn [all-tickers]
                               (pc/map-vals
                                 (fn [v]
                                   (if (map? v)
                                     (->> (pm/safe-select-keys v [:last :highestBid :lowestAsk])
                                          (pc/map-vals #(Double/parseDouble %)))
                                     v))
                                 all-tickers))
            diffs (->> out
                       utils/to-seq
                       (filter identity)
                       (map parse-price-info)
                       (cons {})
                       utils/pairs
                       (map #(utils/diff-map (first %) (second %))))]
        (doseq [diff diffs]
          (log/debug "Recording ticker diff:" diff)
          (db/record-ticker diff)))
      out)))

(def actions
  {:lending-rates #(utils/every (* 1000 60 15) (partial loan-control "BTC" false))
   :lender #(utils/every (* 1000 60 15) (partial loan-control "BTC" true))
   :ticker #(poll-ticker (* 1000 1))})

#_(defonce config (atom #{}))

#_(defstate jobs
    :start (doall (map #(%) (keep actions @config)))
    :stop (dorun (map a/close! jobs)))
