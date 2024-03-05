(ns swidden.page
  (:require [swidden.util :as util]
            [hickory.core :as h]
            [hickory.select :as s]
            [hickory.zip :as hz]
            [clojure.zip :as cz]))

(defn get-htree
  "Takes an html string and parses to hickory"
  [html]
  (-> html
      (h/parse)
      (h/as-hickory)))

(defn- get-results-count-str
  [htree]
  (let [pattern (re-pattern #"(\d+)-(\d+) of (over )?((\d+,?)+) results for")]
    (-> (s/select-next-loc
         (s/descendant (s/attr :data-component-type
                               (util/match "s-result-info-bar"))
                       (s/find-in-text pattern))
         (hz/hickory-zip htree))
        cz/down
        cz/node)))

(defn get-results-number
  "Takes a hickory tree (see [[get-htree]]) and returns an integer indicating
  the number of results on the page (does not include sponsors)"
  [htree]
  (let [pattern (re-pattern #"(\d+)-(\d+) of (over )?((\d+,?)+) results for")
        count-str (get-results-count-str htree)
        matches (re-matches pattern count-str)
        range-start (Integer/parseInt (nth matches 1))
        range-end (Integer/parseInt (nth matches 2))]
    (- range-end (dec range-start))))

(defn get-results
  "Takes a hickory tree (see [[get-htree]]) and returns a vector containing
  the hickory tree for each result on the page"
  [htree]
  (s/select (s/attr :data-component-type
                    (util/match "s-search-result"))
            htree))

(defn get-info-container
  "Takes a search result (see [[get-results]]) and returns the tree node
  containing the product recipes and information."
  [result]
  (-> (s/select-next-loc
       (s/and (s/tag :div)
              (s/attr :data-cy
                      (util/match "title-recipe")))
       (hz/hickory-zip result))
      cz/up
      cz/node))
