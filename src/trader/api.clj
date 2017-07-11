(ns trader.api
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [trader.constants :as consts]
            [trader.crypto :as crypto]
            [trader.db :as db]
            [trader.utils :as utils]))

(def api-key (System/getenv "POLONIEX_API_KEY"))
(def secret (System/getenv "POLONIEX_SECRET"))

(def poloniex-push-endpoint "wss://api.poloniex.com")
(def poloniex-public-endpoint "https://poloniex.com/public")
(def poloniex-trade-endpoint "https://poloniex.com/tradingApi")

(defn init []
  (crypto/configure-mac! secret))

(defn- api-get* [data]
  (->
    (http/get (format "%s?%s" poloniex-public-endpoint (utils/url-encode-map data))
              {:as :json})
    :body))

(defn- api-post* [data]
  (let [nonce (System/currentTimeMillis)
        post-body (utils/url-encode-map (assoc data :nonce nonce))
        signed-data (crypto/hmac-sha512-hex post-body)]
    (->
      (http/post poloniex-trade-endpoint
                 {:headers {"Key" api-key
                            "Sign" signed-data
                            "Content-Type" "application/x-www-form-urlencoded"}
                  :body post-body
                  :as :json})
      :body)))

(def api-get (utils/throttle api-get* 6 1000))
(def api-post (utils/throttle api-post* 6 1000))

;; Public API Methods
(defn return-ticker []
  (api-get {:command "returnTicker"}))

(defn return-24-volume []
  (api-get {:command "return24Volume"}))

(defn return-order-book [currency-pair depth]
  (consts/validate-currency-pair currency-pair "all")
  (api-get {:command "returnOrderBook"
            :currencyPair currency-pair
            :depth depth}))

(defn return-trade-history [currency-pair start end]
  (consts/validate-currency-pair currency-pair)
  (api-get {:command "returnTradeHistory"
            :currencyPair currency-pair
            :start start
            :end end}))

(defn return-chart-data [currency-pair start end period]
  (consts/validate-currency-pair currency-pair)
  (consts/validate-chart-period period)
  (api-get {:command "returnChartData"
            :currencyPair currency-pair
            :start start
            :end end
            :period period}))

(defn return-currencies []
  (api-get {:command "returnCurrencies"}))

(defn return-loan-orders [currency]
  (consts/validate-currency currency)
  (api-get {:command "returnLoanOrders"
            :currency currency}))

;; Trading API Methods
(defn return-balances []
  (api-post {:command "returnBalances"}))

(defn return-complete-balances [account]
  (consts/validate-account account nil)
  (api-post {:command "returnCompleteBalances"
             :account account}))

(defn return-deposit-addresses []
  (api-post {:command "returnDepositAddresses"}))

(defn generate-new-address [currency]
  (consts/validate-currency currency)
  (api-post {:command "generateNewAddress"
             :currency currency}))

(defn return-deposits-withdrawals [start end]
  (api-post {:command "returnDepositsWithdrawals"
             :start start
             :end end}))

(defn return-open-orders [currency-pair]
  (consts/validate-currency-pair currency-pair)
  (api-post {:command "returnOpenOrders"
             :currencyPair currency-pair}))

(defn return-trade-history [currency-pair start end]
  (consts/validate-currency-pair currency-pair "all")
  (api-post {:command "returnTradeHistory"
             :start start
             :end end}))

(defn return-order-trades [order-number]
  (api-post {:command "returnOrderTrades"
             :orderNumber order-number}))

(defn buy [currency-pair rate amount fill-or-kill immediate-or-cancel post-only]
  (consts/validate-currency-pair currency-pair)
  (api-post {:command "buy"
             :currencyPai currency-pair
             :fillOrKill fill-or-kill
             :immediateOrCancel immediate-or-cancel
             :postOnly post-only}))

(defn sell [currency-pair rate amount fill-or-kill immediate-or-cancel post-only]
  (consts/validate-currency-pair currency-pair)
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
  (consts/validate-currency currency)
  (api-post {:command "withdraw"
             :amount amount
             :address address
             :paymentId payment-id}))

(defn return-fee-info []
  (api-post {:command "returnFeeInfo"}))

(defn return-available-account-balances [account]
  (consts/validate-account account nil)
  (api-post {:command "returnAvailableAccountBalances"
             :account account}))

(defn return-tradable-balances []
  (api-post {:command "returnTradableBalances"}))

(defn transfer-balance [currency amount from-account to-account]
  (consts/validate-currency currency)
  (consts/validate-account from-account)
  (consts/validate-account to-account)
  (api-post {:command "transferBalance"
             :amount amount
             :fromAccount from-account
             :toAccount to-account}))

(defn get-margin-account-summary []
  (api-post {:command "returnMarginAccountSummary"}))

(defn margin-buy [currency-pair rate amount lending-rate]
  (consts/validate-currency-pair currency-pair)
  (api-post {:command "marginBuy"
             :currencyPair currency-pair
             :rate rate
             :amount amount
             :lendingRate lending-rate}))

(defn margin-sell [currency-pair rate amount lending-rate]
  (consts/validate-currency-pair currency-pair)
  (api-post {:command "marginSell"
             :currencyPair currency-pair
             :rate rate
             :amount amount
             :lendingRate lending-rate}))

(defn get-margin-position [currency-pair]
  (consts/validate-currency-pair currency-pair "all")
  (api-post {:command "getMarginPosition"
             :currencyPair currency-pair}))

(defn close-margin-position [currency-pair]
  (consts/validate-currency-pair currency-pair)
  (api-post {:command "closeMarginPosition"
             :currencyPair currency-pair}))

(defn create-loan-offer [currency amount duration auto-renew rate]
  (consts/validate-currency currency)
  (api-post {:command "createLoanOffer"
             :currency currency
             :amount amount
             :duration duration
             :autoRenew auto-renew
             :lendingRate rate}))

(defn cancel-loan-offer [order-number]
  (api-post {:command "cancelLoanOffer"
             :orderNumber order-number}))

;; NOTE: These two routes apparently need a valid `currency` specified, even though they return all anyway.
(defn return-open-loan-offers []
  (api-post {:comand "returnOpenLoanOffers"
             :currency "BTC"}))

(defn return-active-loan-offers []
  (api-post {:comand "returnActiveLoans"
             :currency "BTC"}))

(defn return-lending-history [start end]
  (api-post {:comand "returnLendingHistory"
             :start start
             :end end}))

(defn toggle-auto-renew [order-number]
  (api-post {:command "toggleAutoRenew"
             :orderNumber order-number}))
