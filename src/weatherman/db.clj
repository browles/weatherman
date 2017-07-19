(ns weatherman.db
  (:require [clj-time.coerce :as c]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]))

(def schema-file "db/schema.sql")
(def db-file "db/poloniex.db")

(def ^:dynamic *poloniex-db* {:classname "org.sqlite.JDBC"
                              :subprotocol "sqlite"
                              :subname "db/poloniex.db"})

(def ^:dynamic *poloniex-conn*)

(def db-lock (Object.))

(defmacro with-db-transaction [& body]
  `(jdbc/with-db-transaction [conn# *poloniex-db*]
     (locking db-lock
       (binding [*poloniex-conn* conn#]
         ~@body))))

;; cljfmt doesn't like "rowid()"
(def last-insert-rowid (keyword "last_insert_rowid()"))

(defn query [& args]
  (with-db-transaction
    (apply jdbc/query *poloniex-conn* args)))

(defn execute [& args]
  (with-db-transaction
    (apply jdbc/execute! *poloniex-conn* args)))

(defn get-last-row [table]
  (-> (format "SELECT * FROM %s ORDER BY id DESC LIMIT 1;" (name table))
      query
      first))

(defn insert [table & args]
  (with-db-transaction
    (apply jdbc/insert! *poloniex-conn* table args)))

(defn select
  ([table where-fields]
   (select table where-fields ""))
  ([table where-fields suffix]
   (let [where-str (->> where-fields
                        (map (fn [[k v]]
                               (format "%s = %s" (name k) v)))
                        (string/join ", "))]
     (-> (format "SELECT * FROM %s WHERE %s %s;"
                 (name table)
                 where-str
                 suffix)
         query))))

(def insert-market-loan-offer-ingestion (partial insert :market_loan_offer_ingestions))
(def insert-market-loan-offer (partial insert :market_loan_offers))
(def insert-loan-offer (partial insert :loan_offers))
(def insert-ticker-ingestion (partial insert :ticker_ingestions))
(def insert-ticker (partial insert :ticker))

(defn record-market-loan-offers [offers]
  (with-db-transaction
    (let [ingestion-id (-> {:id nil}
                           insert-market-loan-offer-ingestion
                           first
                           last-insert-rowid)]
      (->> offers
           (map #(assoc % :ingestion_id ingestion-id))
           (map insert-market-loan-offer)
           dorun))))

(defn record-ticker [ticker]
  (with-db-transaction
    (let [ingestion-id (->> {:api_start (c/to-timestamp (:api-start ticker))
                             :api_end (c/to-timestamp (:api-end ticker))}
                            insert-ticker-ingestion
                            first
                            last-insert-rowid)]
      (->> (dissoc ticker :api-start :api-end)
           (map (fn [[k v]]
                  {:ingestion_id ingestion-id
                   :currency_pair (name k)
                   :last (:last v)
                   :highest_bid (:highestBid v)
                   :lowest_ask (:lowestAsk v)}))
           (map insert-ticker)
           dorun))))
