(ns weatherman.core
  (:require [clojure.core.async :as a]
            [clojure.tools.logging :as log]
            [clojure.tools.namespace :as tn]
            [mount.core :as mount]
            [plumbing.core :as pc]
            [plumbing.map :as pm]
            [weatherman.api :as api]
            [weatherman.db :as db]
            [weatherman.jobs :as jobs]
            [weatherman.utils :as utils])
  (:gen-class))

(defn -main
  [& args]
  (log/info "Starting.")
  (log/debug "Debugging...")
  (api/init)
  ;; (swap! jobs/config #(into % args))
  (let [l (:lender jobs/actions)]
    (l)
    @(promise)))
