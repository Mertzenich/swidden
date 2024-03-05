(ns swidden.util
  (:require [clojure.zip :as cz]))

(defn match
  "Takes a value `v` and returns a unary function that tests
  for equality with value `v`"
  [v]
  (partial = v))

(defmacro when-let*
  {:clj-kondo/lint-as 'clojure.core/let}
  ([bindings & body]
   (if (seq bindings)
     `(when-let [~(first bindings) ~(second bindings)]
        (when-let* ~(drop 2 bindings) ~@body))
     `(do ~@body))))

(defn get-node-content
  [loc]
  (when-let* [loc loc
              node (cz/node loc)
              content-vec (:content node)
              content (first content-vec)
              _ (string? content)]
             content))
