(defproject weatherman "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["localrepo" "file:local_maven"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.3.443"]
                 [org.clojure/java.jdbc "0.6.1"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [clj-http "3.6.1"]
                 [cheshire "5.7.1"]
                 [environ "1.1.0"]
                 [clj-time/clj-time "0.14.0"]
                 [prismatic/plumbing "0.5.4"]
                 [local/autobahn "17.7.1"]
                 [org.xerial/sqlite-jdbc "3.19.3"]
                 [io.netty/netty-all "4.1.13.Final"]
                 [org.slf4j/slf4j-api "1.8.0-alpha2"]
                 [org.slf4j/slf4j-log4j12 "1.8.0-alpha2"]
                 [org.knowm.xchange/xchange-core "4.2.0"]
                 [org.knowm.xchange/xchange-poloniex "4.2.0"]
                 [org.knowm.xchange/xchange-coinbase "4.2.0"]]
  :main weatherman.core
  :aot :all
  :jvm-opts ["-Xmx8g" "-Xms8g" "-server" "-XX:-OmitStackTraceInFastThrow" "-DLogLevel=INFO" "-DLogDirectory=./log"]
  :aliases {"cli" ["run" "-m" "weatherman.cli"]}
  :repl-options {:init-ns weatherman.core
                 :init (require 'weatherman.core)}
  :plugins [[lein-shell "0.5.0"]
            [lein-pprint "1.1.2"]
            [lein-environ "1.1.0"]]
  :global {*warn-on-reflection* true}
  :target-path "target/%s"
  :profiles {:debug {:jvm-opts ["-DLogLevel=DEBUG"]}})
