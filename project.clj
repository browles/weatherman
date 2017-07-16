(defproject weatherman "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["localrepo" "file:local_maven"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.3.443"]
                 [org.clojure/java.jdbc "0.6.1"]
                 [clj-http "3.6.1"]
                 [cheshire "5.7.1"]
                 [environ "1.1.0"]
                 [local/autobahn "17.7.1"]
                 [org.xerial/sqlite-jdbc "3.19.3"]
                 [io.netty/netty-all "4.1.13.Final"]
                 [org.slf4j/slf4j-api "1.8.0-alpha2"]
                 [org.knowm.xchange/xchange-core "4.2.0"]
                 [org.knowm.xchange/xchange-poloniex "4.2.0"]
                 [org.knowm.xchange/xchange-coinbase "4.2.0"]]
  :main weatherman.core
  :repl-options {:init-ns weatherman.core
                 :init (require 'weatherman.core)}
  :plugins [[lein-shell "0.5.0"]
            [lein-pprint "1.1.2"]
            [lein-environ "1.1.0"]]
  :global {*warn-on-reflection* true}
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
