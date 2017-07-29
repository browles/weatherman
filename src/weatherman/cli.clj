(ns weatherman.cli
  (:require [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [weatherman.core :as core]
            [weatherman.db :as db])
  (:import [java.lang AssertionError]
           [org.sqlite SQLiteException])
  (:gen-class))

(def migration-regex #"(?m)^\s*-- \[([0-9]+)\](.*)$")

(defn- parse-migrations [schema-file]
  (let [text (slurp schema-file)
        migrations (->> text
                        (re-seq migration-regex)
                        (map #(hash-map :id (Integer/parseInt (nth % 1))
                                        :message (string/trim (nth % 2)))))
        commands (-> text
                     (string/split migration-regex)
                     (->> (map string/trim))
                     (->> (filter (comp pos? count)))
                     (->> (map #(hash-map :command %))))]
    (assert (= (count migrations) (count commands)) "Number of commands and migrations do not match.")
    (->>
      (map vector migrations commands)
      (map #(apply merge %)))))

(defn- run-migrations [schema-file]
  (try
    (let [last-migration (get (db/get-last-row :migrations) :id 0)
          migrations (parse-migrations schema-file)
          ids (map :id migrations)]
      (assert (= ids (->> migrations (sort-by :id <) (map :id))) "Migrations are out of order.")
      (assert (= ids (->> migrations count range (map inc))) "Migrations must be consecutive and start at 1.")
      (doseq [migration (filter #(> (:id %) last-migration) migrations)]
        (println (format "Applying migration [%s]: %s" (:id migration) (:message migration)))
        (db/with-db-transaction
          (doseq [command (string/split (:command migration) #";")]
            (db/execute command))
          (db/insert :migrations (select-keys migration [:message])))))
    (catch AssertionError e
      (println "Error in migrations file:" (.getMessage e)))
    (catch SQLiteException e
      (println "Error executing SQL:" (.getMessage e)))))

(def cli-options
  [[nil "--schema FILE" "Path to the schema migrations file."
    :default db/schema-file
    :validate [seq "--schema specified but no file provided."]]
   [nil "--lender"]
   [nil "--lending-rates"]
   [nil "--ticker"]
   ["-h" "--help"]])

(defn- usage [summary]
  (->> ["weatherman.cli"
        ""
        "Usage: action [options]"
        ""
        "Options:"
        summary
        ""
        "Actions:"
        "  run-migrations    Run pending schema migrations. Requires the '--schema' option."]
       (string/join \newline)))

(defn- exit [status message]
  (println message)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [options arguments summary errors]} (cli/parse-opts args cli-options)]
    (cond (:help options) (exit 0 (usage summary))
          errors (exit 1 (string/join \newline errors))
          :else (case (first arguments)
                  "run-migrations" (run-migrations (:schema options))
                  "job" (apply core/-main (keys options))
                  (exit 1 (usage summary))))))
