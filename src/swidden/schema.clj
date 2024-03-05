(ns swidden.schema
  (:require [malli.core :as m]))

(def Authors
  (m/schema
   [:or
    :tuple
    [:tuple :string]
    [:tuple :string :string]
    [:tuple :string :string [:enum :et-al]]]))
