(ns weatherman.cli
  (:require [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [weatherman.db :as db])
  (:import [java.lang AssertionError])
  (:gen-class))

(def migration-regex #"^-- \[([0-9]+)\](.*)")

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
      (assert (= ids (->> migrations (sort-by :id <) (map :ids))) "Migrations are out of order.")
      (assert (= ids (->> migrations count range (map inc))) "Migrations must be consecutive and start at 1.")
      (doseq [migration (filter #(> (:id %) last-migration) migrations)]
        (println (format "Applying migration [%s]: %s" (:id migration) (:message migration)))
        (db/with-db-transaction
          (db/execute (:command migration))
          (db/insert :migrations {:message migration}))))
    (catch AssertionError e
      (prn "Migration failed for reason:" (.getMessage e)))))

(def cli-options
  [[nil "--schema FILE" "Path to the schema migrations file."
    :validate [seq "No schema provided."]]
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
  (throw (Exception. "asdf"))
  (System/exit status))

(defn -main [& args]
  (let [{:keys [options arguments summary errors]} (cli/parse-opts args cli-options)]
    (cond (:help options) (exit 0 (usage summary))
          errors (exit 1 (string/join \newline errors))
          :else (case (first arguments)
                  "run-migrations" (run-migrations (:schema options))
                  (exit 0 (usage summary))))))
