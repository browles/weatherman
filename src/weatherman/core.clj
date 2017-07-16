(ns weatherman.core
  (:require [clojure.core.async :as a]
            [weatherman.api :as api]
            [weatherman.db :as db]
            [weatherman.utils :as utils])
  (:gen-class))

(defn process-current-market-loan-offers [currency]
  (println "Fetching current loan offers for" currency)
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
    (/ (reduce + bottom-rates)
       (count bottom-rates))))

(defn create-loan [currency]
  (process-current-market-loan-offers currency)
  (println "Fetching lending account balance for" currency)
  (let [balance (-> (api/return-available-account-balances "lending")
                    (get-in [:lending (keyword currency)] "0.00")
                    (#(Double/parseDouble %)))
        threshold 0.01
        amount (utils/truncate-float (min balance 0.25))
        duration 2
        rate (utils/truncate-float (choose-rate))]
    (when (>= amount threshold)
      (println "Attempting to create offer for" amount currency "at" (utils/format-float rate))
      (let [result (api/create-loan-offer "BTC"
                                          amount
                                          duration
                                          0
                                          rate)]
        (if (zero? (:success result))
          (println "Failed creating offer for reason:" result)
          (do
            (println "Created offer for" amount "at" (utils/format-float rate))
            (db/insert-loan-offer {:order_id (:orderID result)
                                   :amount amount
                                   :duration duration
                                   :rate rate})))))))

(defn poll-ticker [period]
  (let [get-ticker (utils/throttle api/return-ticker 1 period)
        out (utils/repeatedly-chan get-ticker 5)]
    (utils/safe-thread
      (let [parse-price-info (fn [t]
                               (into {} (map (fn [[k v]]
                                               (if (map? v)
                                                 [k (select-keys v [:last :highestBid :lowestAsk])]
                                                 [k v]))
                                             t)))
            diffs (->> out
                       utils/to-seq
                       (map parse-price-info)
                       utils/pairs
                       (map #(apply utils/diff-map %)))]
        (doseq [diff (take 10 diffs)]
          (prn diff (:start-delta diff) (:end-delta diff))))
      out)))

(defn -main
  [& args]
  (api/init)
  (let [lender (utils/every (* 1000 60 15) #(create-loan "BTC"))
        ticker (a/chan) #_(poll-ticker (* 1000 1))]
    (try
      @(promise)
      (finally
        (a/close! lender)
        (a/close! ticker)))))
