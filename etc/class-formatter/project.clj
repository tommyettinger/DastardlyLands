(defproject class-formatter "0.1.0-SNAPSHOT"
  :description "Generator for game-usable files from complicated data"
  :license {:name "Apache License 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0.html"}
  :repositories [["jitpack" "https://jitpack.io"]]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/data.json "0.2.6"]
                 [com.github.SquidPony/SquidLib "bc06643e7b"]]
  :main ^:skip-aot class-formatter.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
