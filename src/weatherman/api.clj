(ns weatherman.api
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure.core.async :as a]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [weatherman.crypto :as crypto]
            [weatherman.utils :as utils]
            [weatherman.wamp :as wamp]))

(def api-key (env :poloniex-api-key))
(def secret (env :poloniex-secret))

(def user-agent "weatherman.api.clj/0.0.1")

(def poloniex-push-endpoint "wss://api.poloniex.com")
(def poloniex-public-endpoint "https://poloniex.com/public")
(def poloniex-trade-endpoint "https://poloniex.com/tradingApi")

(def chart-periods (atom #{300 900 1800 7200 14400 86400}))
(def currencies (atom nil))
(def currency-pairs (atom nil))
(def accounts (atom #{"lending" "margin" "exchange"}))

(def validate-currency (partial utils/assert-contains
                                currencies
                                "Invalid currency provided: %s"))
(def validate-currency-pair (partial utils/assert-contains
                                     currency-pairs
                                     "Invalid currency-pair provided: %s"))
(def validate-chart-period (partial utils/assert-contains
                                    chart-periods
                                    "Invalid period provided: %s"))
(def validate-account (partial utils/assert-contains
                               accounts
                               "Invalid account provided: %s"))

(defn- api-get* [data]
  (let [start (System/currentTimeMillis)]
    (->
      (http/get (format "%s?%s" poloniex-public-endpoint (utils/url-encode-map data))
                {:headers {"User-Agent" user-agent}
                 :as :json})
      :body
      (assoc :api-start start
             :api-end (System/currentTimeMillis)))))

(defn- api-post* [data]
  (let [nonce (System/currentTimeMillis)
        post-body (utils/url-encode-map (assoc data :nonce nonce))
        signed-data (crypto/hmac-sha512-hex post-body)
        start (System/currentTimeMillis)]
    (->
      (http/post poloniex-trade-endpoint
                 {:headers {"Content-Type" "application/x-www-form-urlencoded"
                            "Key" api-key
                            "Sign" signed-data
                            "User-Agent" user-agent}
                  :body post-body
                  :as :json})
      :body
      (assoc :api-start start
             :api-end (System/currentTimeMillis)))))

(def api-get (utils/throttle api-get* 6 1000))
(def api-post (utils/throttle api-post* 6 1000))

;; Public API Methods
(defn return-ticker []
  (api-get {:command "returnTicker"}))

(defn return-24-volume []
  (api-get {:command "return24Volume"}))

(defn return-order-book
  ([]
   (return-order-book "all"))
  ([currency-pair depth]
   (validate-currency-pair currency-pair :allow "all")
   (api-get {:command "returnOrderBook"
             :currencyPair currency-pair
             :depth depth})))

(defn return-trade-history [currency-pair start end]
  (validate-currency-pair currency-pair)
  (api-get {:command "returnTradeHistory"
            :currencyPair currency-pair
            :start start
            :end end}))

(defn return-chart-data [currency-pair start end period]
  (validate-currency-pair currency-pair)
  (validate-chart-period period)
  (api-get {:command "returnChartData"
            :currencyPair currency-pair
            :start start
            :end end
            :period period}))

(defn return-currencies []
  (api-get {:command "returnCurrencies"}))

(defn return-loan-orders [currency]
  (validate-currency currency)
  (api-get {:command "returnLoanOrders"
            :currency currency}))

;; Trading API Methods
(defn return-balances []
  (api-post {:command "returnBalances"}))

(defn return-complete-balances [account]
  (validate-account account :allow nil)
  (api-post {:command "returnCompleteBalances"
             :account account}))

(defn return-deposit-addresses []
  (api-post {:command "returnDepositAddresses"}))

(defn generate-new-address [currency]
  (validate-currency currency)
  (api-post {:command "generateNewAddress"
             :currency currency}))

(defn return-deposits-withdrawals [start end]
  (api-post {:command "returnDepositsWithdrawals"
             :start start
             :end end}))

(defn return-open-orders [currency-pair]
  (validate-currency-pair currency-pair)
  (api-post {:command "returnOpenOrders"
             :currencyPair currency-pair}))

;; NOTE: "my" to resolve a naming confilct.
(defn return-my-trade-history [currency-pair start end]
  (validate-currency-pair currency-pair :allow "all")
  (api-post {:command "returnTradeHistory"
             :start start
             :end end}))

(defn return-order-trades [order-number]
  (api-post {:command "returnOrderTrades"
             :orderNumber order-number}))

(defn buy [currency-pair rate amount fill-or-kill immediate-or-cancel post-only]
  (validate-currency-pair currency-pair)
  (api-post {:command "buy"
             :currencyPai currency-pair
             :fillOrKill fill-or-kill
             :immediateOrCancel immediate-or-cancel
             :postOnly post-only}))

(defn sell [currency-pair rate amount fill-or-kill immediate-or-cancel post-only]
  (validate-currency-pair currency-pair)
  (api-post {:command "sell"
             :currencyPai currency-pair
             :fillOrKill fill-or-kill
             :immediateOrCancel immediate-or-cancel
             :postOnly post-only}))

(defn move-order [order-number rate amount post-only immediate-or-cancel]
  (api-post {:command "moveOrder"
             :orderNumber order-number
             :rate rate
             :amount amount
             :postOnly post-only
             :immediateOrCancel immediate-or-cancel}))

(defn withdraw [currency amount address payment-id]
  (validate-currency currency)
  (api-post {:command "withdraw"
             :amount amount
             :address address
             :paymentId payment-id}))

(defn return-fee-info []
  (api-post {:command "returnFeeInfo"}))

(defn return-available-account-balances
  ([] (return-available-account-balances nil))
  ([account]
   (validate-account account :allow nil)
   (api-post {:command "returnAvailableAccountBalances"
              :account account})))

(defn return-tradable-balances []
  (api-post {:command "returnTradableBalances"}))

(defn transfer-balance [currency amount from-account to-account]
  (validate-currency currency)
  (validate-account from-account)
  (validate-account to-account)
  (api-post {:command "transferBalance"
             :amount amount
             :fromAccount from-account
             :toAccount to-account}))

(defn get-margin-account-summary []
  (api-post {:command "returnMarginAccountSummary"}))

(defn margin-buy [currency-pair rate amount lending-rate]
  (validate-currency-pair currency-pair)
  (api-post {:command "marginBuy"
             :currencyPair currency-pair
             :rate rate
             :amount amount
             :lendingRate lending-rate}))

(defn margin-sell [currency-pair rate amount lending-rate]
  (validate-currency-pair currency-pair)
  (api-post {:command "marginSell"
             :currencyPair currency-pair
             :rate rate
             :amount amount
             :lendingRate lending-rate}))

(defn get-margin-position
  ([]
   (get-margin-position "all"))
  ([currency-pair]
   (validate-currency-pair currency-pair :allow "all")
   (api-post {:command "getMarginPosition"
              :currencyPair currency-pair})))

(defn close-margin-position [currency-pair]
  (validate-currency-pair currency-pair)
  (api-post {:command "closeMarginPosition"
             :currencyPair currency-pair}))

(defn create-loan-offer [currency amount duration auto-renew rate]
  (validate-currency currency)
  (api-post {:command "createLoanOffer"
             :currency currency
             :amount amount
             :duration duration
             :autoRenew auto-renew
             :lendingRate rate}))

(defn cancel-loan-offer [order-number]
  (api-post {:command "cancelLoanOffer"
             :orderNumber order-number}))

(defn return-open-loan-offers []
  (api-post {:command "returnOpenLoanOffers"}))

(defn return-active-loan-offers []
  (api-post {:command "returnActiveLoans"}))

(defn return-lending-history [start end]
  (api-post {:comand "returnLendingHistory"
             :start start
             :end end}))

(defn toggle-auto-renew [order-number]
  (api-post {:command "toggleAutoRenew"
             :orderNumber order-number}))

;; Push API
(defn- poloniex-ticker-parser [[[currencyPair last lowestAsk highestBid percentChange
                                 baseVolume quoteVolume isFrozen hr24High hr24Low]]]
  {:currencyPair currencyPair
   :last (Double/parseDouble last)
   :lowestAsk (Double/parseDouble lowestAsk)
   :highestBid (Double/parseDouble highestBid)
   :percentChange (Double/parseDouble percentChange)
   :baseVolume (Double/parseDouble baseVolume)
   :quoteVolume (Double/parseDouble quoteVolume)
   :isFrozen (pos? isFrozen)
   :24hrHigh (Double/parseDouble hr24High)
   :24hrLow (Double/parseDouble hr24Low)})

(defn ticker []
  (let [connection (wamp/connect poloniex-push-endpoint "realm1")
        out (a/chan 1000 (map poloniex-ticker-parser))]
    (a/go
      (loop [e (a/<! (:events connection))]
        (case (:type e)
          :connect (println "Connected!")
          :disconnect (println "Disconnected!" (:data e))
          :ready (println "Ready!")
          :join (do
                  (println "Joined!" (:data e))
                  (a/pipe (wamp/subscribe (:session connection) "ticker") out))
          :leave (println "Left!" (:data e))
          :user-error (println "User-error!" (:data e)))
        (recur (a/<! (:events connection)))))
    out))

;; Initialization
(defn configure! []
  (letfn [(configure [atom api-call]
            (reset! atom (->> api-call keys (map name) set)))]
    (configure currencies (return-currencies))
    (configure currency-pairs (return-ticker))))

(defn init []
  (try
    (configure!)
    (assert api-key "Poloniex API key not set!")
    (assert secret "Poloniex secret not set!")
    (crypto/configure-mac! secret)
    (catch Exception e
      (log/fatal "Error initializing API." e))))
