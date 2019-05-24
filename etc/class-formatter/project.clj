(defproject class-formatter "0.1.0-SNAPSHOT"
  :description "Generator for role JSON from complicated data"
  :url "http://example.com/FIXME"
  :license {:name "Apache License 2.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
  [org.clojure/data.json "0.2.6"]]
  :main ^:skip-aot class-formatter.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
