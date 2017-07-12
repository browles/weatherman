(ns weatherman.db
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]))

(def poloniex-db {:classname "org.sqlite.JDBC"
                  :subprotocol "sqlite"
                  :subname "db/poloniex.db"})

(def ^:dynamic *poloniex-conn*)

(defmacro with-db-transaction [& body]
  `(jdbc/with-db-transaction [conn# poloniex-db]
     (binding [*poloniex-conn* conn#]
       ~@body)))

;; cljfmt doesn't like "rowid()"
(def last-insert-rowid (keyword "last_insert_rowid()"))

(defn query [str]
  (with-db-transaction
    (jdbc/query *poloniex-conn* str)))

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

(defn record-market-loan-offers [offers]
  (with-db-transaction
    (let [ingestion-id (-> {:id nil}
                           insert-market-loan-offer-ingestion
                           first
                           last-insert-rowid)]
      (->> offers
           (map #(assoc % :ingestion_id ingestion-id))
           (map insert-market-loan-offer)
           doall))))
