(ns trader.api
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [trader.crypto :as crypto]
            [trader.db :as db]
            [trader.utils :as utils]))

(def api-key (System/getenv "POLONIEX_API_KEY"))
(def secret (System/getenv "POLONIEX_SECRET"))

(def poloniex-get-endpoint "https://poloniex.com/public")
(def poloniex-post-endpoint "https://poloniex.com/tradingApi")

(crypto/configure-mac! secret)

(defn api-get [data]
  (->
    (http/get (format "%s?%s" poloniex-get-endpoint (utils/url-encode-map data))
              {:as :json})
    :body))

(defn api-post [data]
  (let [nonce (System/currentTimeMillis)
        post-body (utils/url-encode-map (assoc data :nonce nonce))
        signed-data (crypto/hmac-sha512-hex post-body)]
    (->
      (http/post poloniex-post-endpoint
                 {:headers {"Key" api-key
                            "Sign" signed-data
                            "Content-Type" "application/x-www-form-urlencoded"}
                  :body post-body
                  :as :json})
      :body)))

(defn get-market-loan-orders [currency]
  (api-get {:command "returnLoanOrders"
            :currency currency}))

(defn get-lending-account-balance []
  (api-post {:command "returnAvailableAccountBalances"
             :account "lending"}))

(defn create-loan-offer [currency amount duration auto-renew rate]
  (api-post {:command "createLoanOffer"
             :currency currency
             :amount amount
             :duration duration
             :autoRenew auto-renew
             :lendingRate rate}))
