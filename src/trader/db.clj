(ns trader.db
  (:require [clojure.java.jdbc :as jdbc]))

(def db {:classname "org.sqlite.JDBC"
         :subprotocol "sqlite"
         :subname "db/trader.db"})

(defn insert [table row]
  (jdbc/insert! db table row))

(def insert-loan-order-ingestions (partial insert :loan_order_ingestions))
(def insert-loan-order (partial insert :loan_orders))

(defn query [str]
  (jdbc/query db str))
