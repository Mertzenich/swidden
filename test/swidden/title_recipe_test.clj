(ns swidden.title-recipe-test
  (:require [malli.core :as m]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [swidden.schema :as schema]
            [swidden.page :as pg]
            [swidden.title-recipe :as tr]
            [clojure.test :refer [deftest testing is]]))

;; TODO: Write a test that checks: (# actual results) - (# sponsored) = # Expected

;; (deftest a-test
;;   (testing "TEST"
;;     (is (= true true))))

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

(deftest get-authors
  (testing "Return authors of a book"
    (let [authors (mapv tr/get-authors all-results)]
      ;; Check that every element of the authors vector
      ;; matches the Authors schema
      (is (m/validate [:vector schema/Authors] authors)))))

(deftest get-title
  (testing "Every result has a title"
    (is (every? #(->> %
                      (tr/get-title)
                      ;; Titles must not be empty, must not be only symbols,
                      ;; and must contain alphanumeric characters.
                      (re-matches #"^(?=.*[a-zA-Z0-9]).+$"))
                all-results))))

(deftest get-url
  (let [urls (map tr/get-url all-results)
        sponsors (map tr/is-sponsored? all-results)]
    (testing "Every result has a url"
      (is (every? #(str/starts-with? % "/") urls)))
    (testing "Every url starting with /sspa/ is sponsored"
      (let [urls-sponsors (mapv vector urls sponsors)]
        (is (every? #(if (str/starts-with? (first %) "/sspa/")
                       (true? (second %))
                       (false? (second %)))
                    urls-sponsors))))))
