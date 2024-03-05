(defproject ch.mertzeni/swidden "0.1.1"
  :description "A work-in-progress utility for parsing Amazon search results."
  :url "https://github.com/Mertzenich/swidden"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.cli "1.0.219"]
                 [cheshire/cheshire "5.12.0"]
                 [etaoin/etaoin "1.0.40"]
                 [org.clj-commons/hickory "0.7.5"]
                 [metosin/malli "0.14.0"]]
  :main ^:skip-aot swidden.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev {:dependencies [[spyscope "0.1.6"]]
                   :injections [(require 'spyscope.core)]}
             :kaocha {:dependencies [[lambdaisland/kaocha "1.87.1366"]]}}
  :aliases {"kaocha" ["with-profile" "+kaocha" "run" "-m" "kaocha.runner"]})
