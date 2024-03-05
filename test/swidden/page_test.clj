(ns swidden.page-test
  (:require [swidden.page :as pg]
            [swidden.title-recipe :as tr]
            [clojure.test :refer [deftest testing is]]
            [clojure.edn :as edn]))

(def queries-html
  "Vector of HTML strings containing the results for searches:
  clojure+book, audio+book, cell+phone, espresso+machine,
  shirt, and beauty"
  (-> "test/resources/queries-html.edn"
      (slurp)
      (edn/read-string)))

(deftest result-count
  (testing "Page result number matches number of non-sponsored results"
    (let [htrees (map pg/get-htree queries-html)
          results-numbers (map pg/get-results-number htrees)
          actual-results (map pg/get-results htrees)
          sponsored-count (map #(->> %
                                     (filter tr/is-sponsored?)
                                     (count))
                               actual-results)
          actual-count (map count actual-results)]
      (is (= results-numbers (map - actual-count sponsored-count))))))

