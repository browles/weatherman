(ns trader.db
  (:require [clojure.java.jdbc :as jdbc]))

(def poloniex-db {:classname "org.sqlite.JDBC"
                  :subprotocol "sqlite"
                  :subname "db/poloniex.db"})

(def ^:dynamic *poloniex-conn*)

(defmacro with-db-transaction [& body]
  `(jdbc/with-db-transaction [conn# poloniex-db]
     (binding [*poloniex-conn* conn#]
       ~@body)))

;; cljfmt doesn't like "rowid()"
(def last-rowid-keyword (keyword "last_insert_rowid()"))

(defn query [str]
  (jdbc/query *poloniex-conn* str))

(defn insert [table & args]
  (apply jdbc/insert! *poloniex-conn* table args))

(def insert-loan-offer-ingestion (partial insert :loan_offer_ingestions))
(def insert-loan-offer (partial insert :loan_offers))

(defn record-loan-offers [offers]
  (with-db-transaction
    (let [ingestion-id (-> {:id nil}
                           insert-loan-offer-ingestion
                           first
                           last-rowid-keyword)]
      (->> offers
           (map #(assoc % :ingestion_id ingestion-id))
           (map insert-loan-offer)
           doall))))
