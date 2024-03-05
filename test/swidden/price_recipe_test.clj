(ns swidden.price-recipe-test
  (:require [swidden.page :as pg]
            [swidden.price-recipe :as pr]
            [clojure.test :refer [deftest testing is]]
            [clojure.edn :as edn]))

(def queries-html
  "Vector of HTML strings containing the results for searches:
  clojure+book, audio+book, cell+phone, espresso+machine,
  shirt, and beauty"
  (-> "test/resources/queries-html.edn"
      (slurp)
      (edn/read-string)))

(def all-results
  (->> queries-html
       (map pg/get-htree)
       (map pg/get-results)
       (apply concat)
       (vec)))

(deftest prices
  (testing "Each example has a price"
    ;; FIXME: Re-evaluate this test, can we expect every item
    ;;       to actually list a price? We need more samples.
    (let [htrees (map pg/get-htree queries-html)
          results (map pg/get-results htrees)
          results-count (map count results)
          prices (map #(map pr/get-price %) results)
          prices-count (map count prices)]
      (is (= results-count prices-count)))))

(deftest formats
  (testing "Get format of each result"
    (let [formats (map pr/get-format all-results)]
      (is (every? #(or (nil? %)
                       ;; Alphanumeric + Spaces
                       (re-matches #"^[a-zA-Z0-9 ]+$" %))
                  formats)))))
