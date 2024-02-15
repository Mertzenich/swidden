(defproject amazon-scraper "0.1.0-SNAPSHOT"
  :description "A work-in-progress scraping utility for Amazon search"
  :url "https://github.com/mertzenich/"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.cli "1.0.219"]
                 [cheshire "5.12.0"]
                 [etaoin "1.0.40"]
                 [org.clj-commons/hickory "0.7.4"]]
  :main ^:skip-aot amazon-scraper.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
