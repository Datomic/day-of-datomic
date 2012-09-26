(ns datomic.samples.schema
  (:use datomic.api))

(defn cardinality
  "Returns the cardinality (:db.cardinality/one or
   :db.cardinality/many) of the attribute"
  [db attr]
  (->>
   (q '[:find ?v
        :in $ ?attr
        :where
        [?attr :db/cardinality ?card]
        [?card :db/ident ?v]]
      db attr)
   ffirst))

