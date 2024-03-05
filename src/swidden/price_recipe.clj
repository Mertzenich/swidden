(ns swidden.price-recipe
  (:require [swidden.util :as util]
            [hickory.select :as s]
            [hickory.zip :as hz]
            [clojure.zip :as cz]
            [clojure.edn :as edn]
            [swidden.page :as pg]))

(defn- get-recipe
  "Takes a hickory tree and returns the price-recipe subtree"
  [htree]
  (-> (s/select-next-loc
       (s/and (s/tag :div)
              (s/attr :data-cy (util/match "price-recipe")))
       (hz/hickory-zip htree))
      cz/node))

(defn get-price
  [htree]
  (when-let [loc (s/select-next-loc
                  (s/descendant (s/class "a-price")
                                (s/find-in-text #"^\$(\d{1,3})(,\d{1,3})*(\.\d{1,})?$"))
                  (hz/hickory-zip (get-recipe htree)))]
    (-> loc
        (cz/down)
        (cz/node)))
  ;; (-> (s/select-next-loc
  ;;      (s/descendant (s/class "a-price")
  ;;                    (s/find-in-text #"^\$(\d{1,3})(,\d{1,3})*(\.\d{1,})?$"))
  ;;      (hz/hickory-zip (get-recipe htree)))
  ;;     cz/down
  ;;     cz/node)
  )

(defn get-format
  ;; FIXME: Need to test this on non-book results
  [htree]
  (when-let [loc (s/select-next-loc
                  (s/child (s/tag :div)
                           (s/and (s/tag :a)
                       ;; Any alphanumeric w/ spaces
                                  (s/find-in-text #"^[a-zA-Z0-9 ]+$")))
                  (hz/hickory-zip (get-recipe htree)))]
    (util/get-node-content loc)))

(comment

  ;; (def html-vec (clojure.edn/read-string (slurp "test/resources/queries-html.edn")))
  ;; (def html (nth html-vec 5))
  ;; (def htree (amazon-scraper.page/get-htree html))
  ;; (def results (amazon-scraper.page/get-results htree))
  ;; (def result (first results))
  ;; @(def prices (mapv get-price results))
  ;; (every? some? prices)
  ;; (get-format result)

  ;; (def queries-html
  ;;   "Vector of HTML strings containing the results for searches:
  ;; clojure+book, audio+book, cell+phone, espresso+machine,
  ;; shirt, and beauty"
  ;;   (-> "test/resources/queries-html.edn"
  ;;       (slurp)
  ;;       (edn/read-string)))

  ;; (def all-results
  ;;   (->> queries-html
  ;;        (map pg/get-htree)
  ;;        (map pg/get-results)
  ;;        (apply concat)
  ;;        (vec)))

  ;; (mapv get-format all-results)

  '...)
