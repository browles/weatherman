(ns weatherman.core
  (:require [weatherman.api :as api]
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
                    (get-in [:lending (keyword currency)] "0")
                    (#(Float/parseFloat %)))
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
        (when-not (zero? (:success result))
          (println "Created offer for" amount "at" (utils/format-float rate))
          (db/insert-loan-offer {:order_id (:orderID result)
                                 :amount amount
                                 :duration duration
                                 :rate rate}))))))

(defn -main
  [& args]
  (api/init)
  (let [lender (utils/every (* 1000 60 15) #(create-loan "BTC"))]
    (try
      @(promise)
      (finally
        (utils/cancel lender)))))
