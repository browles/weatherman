(ns weatherman.cli
  (:require [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [weatherman.db :as db])
  (:gen-class))

(defn check-migrations [schema-file]
  (let [last-migration (get (db/get-last-row :migrations) :id 0)
        migrations (->> (slurp schema-file)
                        (re-find #"(?s)-- \[([0-9]+)\]([^\n-]*)[^-]*")
                        (map (fn [[migration id message]]
                               (prn migration id message)
                               {:migration migration
                                :id (Integer/parseInt id)
                                :message message})))]
    (assert (= migrations (sort-by :id < migrations)) "Migrations are out of order.")
    (assert (= (map :num migrations) (range (count migrations))) "Migrations must be consecutive and start at 1.")
    (doseq [mig (filter #(> (:id %) last-migration) migrations)]
      (prn mig)
      (println (format "Applying migration [%s]: %s" (:id mig) (:message mig))))))
