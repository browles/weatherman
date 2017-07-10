(ns trader.core
  (:require [trader.api :as api]
            [trader.db :as db]
            [trader.utils :as utils])
  (:import [org.knowm.xchange BaseExchange Exchange ExchangeFactory ExchangeSpecification]
           [org.knowm.xchange.currency Currency CurrencyPair]
           [org.knowm.xchange.poloniex Poloniex PoloniexAdapters PoloniexAuthenticated PoloniexException PoloniexExchange PoloniexUtils]
           [org.knowm.xchange.poloniex.service PoloniexMarketDataService])
  (:gen-class))

#_(def poloniex (.createExchange ExchangeFactory/INSTANCE (.getName PoloniexExchange)))
#_(def poloniex-marketdata (.getMarketDataService poloniex))

(defmacro wrap-logging [& body]
  `(try
     ~@body
     (catch Exception e#
       (println e#)
       (throw e#))))

(defn process-current-market-loan-offers [currency]
  (wrap-logging
    (println "Fetching current loan offers for" currency)
    (->> (api/get-market-loan-orders currency)
         :offers
         (db/record-market-loan-offers))))

(defn choose-rate []
  (let [last-ingestion (db/get-last-row :market_loan_offer_ingestions)
        last-offers (db/select :market_loan_offers {:ingestion_id (:id last-ingestion)})
        bottom-rates (->> last-offers
                          (sort-by :rate <)
                          (drop 1)
                          (take 5)
                          (map :rate))]
    (/ (reduce + bottom-rates)
       (count bottom-rates))))

(defn create-loan [currency]
  (wrap-logging
    (process-current-market-loan-offers currency)
    (println "Fetching lending account balance for" currency)
    (let [balance (-> (api/get-lending-account-balance)
                      (get-in [:lending (keyword currency)])
                      (#(Float/parseFloat %)))
          threshold 0.01
          amount (min balance 0.05)
          duration 2
          rate (choose-rate)]
      (when (>= amount threshold)
        (println "Attempting to create offer for" amount "at" rate)
        (let [result (api/create-loan-offer "BTC"
                                            (utils/format-float amount)
                                            duration
                                            0
                                            (utils/format-float rate))]
          (when-not (zero? (:success result))
            (println "Created offer for" amount "at" rate)
            (db/insert-loan-offer {:order_id (:orderID result)
                                   :amount (utils/truncate-float amount)
                                   :duration duration
                                   :rate (utils/truncate-float rate)})))))))

(defn -main
  [& args]
  (let [lender (utils/every (* 1000 60 15) #(create-loan "BTC"))]
    (try
      @(promise)
      (finally
        (utils/cancel-scheduler lender)))))
