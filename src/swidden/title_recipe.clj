(ns swidden.title-recipe
  (:require [swidden.util :as util]
            [swidden.page :as pg]
            [hickory.core :as h]
            [hickory.select :as s]
            [hickory.zip :as hz]
            [clojure.zip :as cz]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [malli.core :as m]
            [malli.provider :as mp]))

(defn- get-recipe
  "Takes a hickory tree and returns the title-recipe subtree"
  [htree]
  (-> (s/select-next-loc
       (s/and (s/tag :div)
              (s/attr :data-cy (util/match "title-recipe")))
       (hz/hickory-zip htree))
      cz/node))

(defn is-sponsored?
  [htree]
  (some?
   (s/select-next-loc
    (s/or (s/attr :aria-label
                  (util/match "View Sponsored information or leave ad feedback"))
          (s/find-in-text #"Sponsored")
          (s/find-in-text #"Featured from Amazon brands")
          (s/attr :data-a-popover
                  #(some? (re-find #"product from an Amazon brand" %))))
    (hz/hickory-zip (get-recipe htree)))))

(defn get-title
  [htree]
  (-> (s/select-next-loc
       (s/descendant (s/tag :h2)
                     (s/find-in-text #"^\s*[^ \s].*$"))
       (hz/hickory-zip (get-recipe htree)))
      cz/down
      cz/node))

(defn- get-first-author-loc
  [htree]
  (when-let
   [loc (s/select-next-loc
         (s/child (s/tag :div)
                  (s/and (s/tag :span)
                         (s/find-in-text #"by ")))
         (hz/hickory-zip (get-recipe htree)))]
    (-> loc
        cz/right)))

(defn- zip-right-next-loc
  [loc]
  (when-let [right (cz/right loc)]
    (s/select-next-loc (s/node-type :element)
                       right)))


(defn get-authors
  [htree]
  ;; TODO: Handle "Book X of Y: Description/Series Title"
  (loop [loc (get-first-author-loc htree)
         authors (transient [])]
    (let [content (util/get-node-content loc)]
      (case content
        nil (persistent! authors)
        (" and ", ", ") (recur (zip-right-next-loc loc)
                               authors)
        ", et al." (recur (zip-right-next-loc loc)
                          (conj! authors :et-al))
        (recur (zip-right-next-loc loc)
               (conj! authors content))))))

(defn get-url
  [htree]
  (-> (s/select-next-loc
       (s/child (s/tag :h2)
                     (s/tag :a))
       (hz/hickory-zip (get-recipe htree)))
      cz/node
      (get-in [:attrs :href])))

(comment

  ;; (def h "<div>
  ;;           <span>by </span>
  ;;           <a>Alex Miller</a>
  ;;           <span>, </span>
  ;;           <span>Stuart Halloway</span>
  ;;           <span>, et al.</span>
  ;;           <span></span>
  ;;           <span> | </span>
  ;;           <span></span>
  ;;           <span>Apr 3, 2018</span>
  ;;         </div>")

  ;; (def html-vec (edn/read-string (slurp "test/resources/queries-html.edn")))
  ;; (let [htrees (map pg/get-htree html-vec)
  ;;       actual-results (map pg/get-results htrees)
  ;;       authors (mapv #(mapv get-authors %) actual-results)
  ;;       urls (mapv #(mapv get-url %) actual-results)]
  ;;   urls)

  ;; (def html (nth html-vec 0))
  ;; (def htree (pg/get-htree html))
  ;; (def results (pg/get-results htree))

  ;; (def result (nth results 5))          ;  Dmitri Sotnikov and Scot Brown
  ;; (def result (nth results 3))          ;  Alex miller et al
  ;; (def result (nth results 12))          ;  chas the fiend
  ;; (map get-authors results)
  ;; @(def author (get-authors result))
  ;; (get-authors result)
  ;; (get-title result)

  ;; (get-url result)

  '...)
