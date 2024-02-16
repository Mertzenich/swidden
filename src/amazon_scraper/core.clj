(ns amazon-scraper.core
  (:gen-class)
  (:require [clojure.string :as str]
            [cheshire.core :as json]
            [clojure.tools.cli :as cli]
            [etaoin.api :as e]
            [hickory.core :as h]
            [hickory.select :as s]
            [hickory.zip :as hz]
            [clojure.zip :as cz]))

(defonce ^:private opts {:args ["--incognito"]})

(defn match
  "Takes a value `v` and returns a unary function that tests
  for equality with value `v`"
  [v]
  (partial = v))

(defn get-htree
  "Takes a driver and returns the parsed hickory tree of the current page"
  [driver]
  (-> (e/get-source driver)
      h/parse
      h/as-hickory))

(defn get-results
  "Takes a hickory tree (see [[get-htree]]) and returns a vector containing
  the hickory tree for each result on the page"
  [htree]
  (s/select (s/attr :data-component-type
                    (match "s-search-result"))
            htree))

(defn get-info-container
  "Takes a search result (see [[get-results]]) and returns the tree node
  containing the product recipes and information."
  [result]
  (-> (s/select-next-loc (s/and (s/tag :div)
                                (s/attr :data-cy
                                        (match "title-recipe")))
                         (hz/hickory-zip result))
      cz/up
      cz/node))

(defn get-title-recipe
  "Takes a hickory tree and returns the title-recipe subtree"
  [htree]
  (first (s/select (s/and (s/tag :div) (s/attr :data-cy (match "title-recipe"))) htree)))

(defn title-recipe->sponsored?
  "Returns true if the title-recipe indicates a sponsored item,
  false otherwise."
  [title-recipe]
  ;; TODO: Handle "Featured from Amazon Brands"
  (if (s/select-next-loc (s/descendant (s/tag :div) (s/and (s/tag :span) (s/find-in-text #"Sponsored"))) (hz/hickory-zip title-recipe)) true false))

(defn title-recipe->title-text
  "Takes a title-recipe tree and returns the item title text"
  [title-recipe]
  (-> (s/select (s/child (s/tag :h2)
                         (s/tag :a)
                         (s/tag :span))
                title-recipe)
      first :content first))

(defn title-recipe->url
  "Takes a title-recipe tree and returns the item url path"
  [title-recipe]
  (-> (s/select (s/child (s/tag :h2)
                         (s/tag :a))
                title-recipe)
      first :attrs :href))

(defn title-recipe->author
  "Takes a title-recipe tree and returns the author name"
  [title-recipe]
  ;; TODO: Support multiple authors:
  ;; "by Michael Fogus and Chris Houser" or "by Chas Emerick , Christophe Grand , et al."
  (let [loc (s/select-next-loc (s/child (s/tag :div)
                                        (s/and (s/tag :span)
                                               (s/find-in-text #"by ")))
                               (hz/hickory-zip title-recipe))]
    ;; `loc` will return nil if it cannot find "by "
    ;; This means series (i.e. "Part of: Expert's Voices in Open Source")
    ;; will result in a return value of nil
    ;; TODO: Support series when an author is not specified
    (when loc (-> loc cz/right cz/next cz/node))))

(defn parse-title-recipe
  "Takes a title-recipe and returns a map containing the information
  contained therein"
  [title-recipe]
  {:sponsored (title-recipe->sponsored? title-recipe)
   :title (title-recipe->title-text title-recipe)
   :url (title-recipe->url title-recipe)
   :author (title-recipe->author title-recipe)})

(defn get-price-recipe
  "Takes a hickory tree and returns the price-recipe subtree"
  [htree]
  (first (s/select (s/and (s/tag :div) (s/attr :data-cy (match "price-recipe"))) htree)))

(defn price-recipe->price
  "Takes a price-recipe tree and returns the item price"
  [price-recipe]
  ;; TODO: Handle "Free with Kindle Unlimited membership"
  (let [loc (s/select-next-loc (s/child (s/tag :div)
                                        (s/tag :div)
                                        (s/tag :a)
                                        (s/and (s/tag :span)
                                               (s/class "a-price"))
                                        (s/and (s/tag :span)
                                               (s/class "a-offscreen")))
                               (hz/hickory-zip price-recipe))]
    (when loc (-> loc cz/next cz/node))))

(defn price-recipe->format
  "Takes a price-recipe tree and returns the item format"
  [price-recipe]
  (-> (s/select-next-loc (s/child (s/tag :div)
                                  (s/tag :a))
                         (hz/hickory-zip price-recipe))
      cz/next
      cz/node))

(defn parse-price-recipe
  "Takes a price-recipe and returns a map containing the information
  contained therein"
  [price-recipe]
  {:price (price-recipe->price price-recipe)
   :format (price-recipe->format price-recipe)})

(defn search
  "Return the results of searching for `s` on Amazon"
  [s]
  (e/with-chrome opts driver
    (e/go driver (str "https://www.amazon.com/s?k=" (str/replace s #" " "+")))
    (for [result (-> driver get-htree get-results)]
      (let [info-container (get-info-container result)
            title-recipe (get-title-recipe info-container)
            price-recipe (get-price-recipe info-container)]
        (merge (parse-title-recipe title-recipe)
               (parse-price-recipe price-recipe))))))

(comment
  (def driver (e/chrome opts))

  (e/go driver "https://www.amazon.com/s?k=Clojure")
  (e/go driver "https://www.amazon.com/s?k=Catechism")

  (e/quit driver))

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
        limited-query (if limit (take limit query) query)]
    (println
     (case output
       "json" (json/generate-string limited-query)
       "edn" (pr-str limited-query)))))
