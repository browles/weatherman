(ns trader.core
  (:require [clj-http.client :as http]
            [trader.db :as db])
  (:import [org.knowm.xchange BaseExchange Exchange ExchangeFactory ExchangeSpecification]
           [org.knowm.xchange.currency Currency CurrencyPair]
           [org.knowm.xchange.poloniex Poloniex PoloniexAdapters PoloniexAuthenticated PoloniexException PoloniexExchange PoloniexUtils]
           [org.knowm.xchange.poloniex.service PoloniexMarketDataService])
  (:gen-class))

(def poloniex (.createExchange ExchangeFactory/INSTANCE (.getName PoloniexExchange)))
(def poloniex-marketdata (.getMarketDataService poloniex))

(def poloniex-loan-endpoint "https://poloniex.com/public?command=returnLoanOrders&currency=")

(defn get-loan-orders [currency-string]
  (->
    (http/get (str poloniex-loan-endpoint currency-string) {:as :json})
    :body))

(defn process-curent-loan-orders [currency-string])

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
