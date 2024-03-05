(ns swidden.core
  (:require [etaoin.api :as e]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [swidden.page :as pg]
            [swidden.title-recipe :as tr]
            [swidden.price-recipe :as pr]
            [cheshire.core :as json])
  (:gen-class))

(defonce ^:private opts {:args ["--incognito"]})

(defn search
  "Search and parse results for the `query`"
  [query]
  (let [html (e/with-chrome opts driver
               (e/go driver (str "https://www.amazon.com/s?k=" (str/replace query #" " "+")))
               (e/get-source driver))
        htree (pg/get-htree html)
        results (pg/get-results htree)]
    {:query query
     :total (count results)
     :results
     (vec (for [result results
                :let [info-container (pg/get-info-container result)]]
            {:title (tr/get-title info-container)
             :authors (tr/get-authors info-container)
             :sponsored? (tr/is-sponsored? info-container)
             :url (tr/get-url info-container)
             :price (pr/get-price info-container)
             :format (pr/get-format info-container)}))}))

(comment
  (def driver (e/chrome opts))
  (e/quit driver)

  (e/go driver (str "https://www.amazon.com/s?k=clojure"))

  (def html (e/get-source driver))

  (def htree (pg/get-htree html))

  (def results (pg/get-results htree))

  '...)

(def cli-options
  [["-o" "--output FORMAT" "Output format"
    :default "json"
    :parse-fn #(str/lower-case (str %))]
   ["-l" "--limit NUMBER" "Limit output"
    :parse-fn #(Integer/parseInt %)]
   ["-h" "--help"]])

(defn -main
  [& args]
  (let [{:keys [arguments options]} (cli/parse-opts args cli-options)
        {output :output limit :limit} options
        query (search (first arguments))
        limited-query (if limit
                        (update query :results #(take limit %))
                        query)]
    (println
     (case output
       "json" (json/generate-string limited-query)
       "edn" (pr-str limited-query)))))
