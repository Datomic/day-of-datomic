;   Copyright (c) Metadata Partners, LLC. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns datomic.samples.query
  (:require [datomic.samples.schema :as schema]
            [datomic.api :as d]))

(defn only
  "Return the only item from a query result"
  [query-result]
  (assert (= 1 (count query-result)))
  (assert (= 1 (count (first query-result))))
  (ffirst query-result))

(defprotocol Eid
  (e [_]))

(extend-protocol Eid
  java.lang.Long
  (e [n] n)

  datomic.Entity
  (e [ent] (:db/id ent)))

(defn qe
  "Returns the single entity returned by a query."
  [query db & args]
  (let [res (apply d/q query db args)]
    (d/entity db (only res))))

(defn find-by
  "Returns the unique entity identified by attr and val."
  [db attr val]
  (qe '[:find ?e
        :in $ ?attr ?val
        :where [?e ?attr ?val]]
      db (d/entid db attr) val))

(defn qes
  "Returns the entities returned by a query, assuming that
   all :find results are entity ids."
  [query db & args]
  (->> (apply d/q query db args)
       (mapv (fn [items]
               (mapv (partial d/entity db) items)))))

(defn find-all-by
  "Returns all entities possessing attr."
  [db attr]
  (qes '[:find ?e
         :in $ ?attr
         :where [?e ?attr]]
       db (d/entid db attr)))

(defn qfs
  "Returns the first of each query result."
  [query db & args]
  (->> (apply d/q query db args)
       (mapv first)))

(defn maybe
  "Returns the value of attr for e, or if-not if e does not possess
   any values for attr. Cardinality-many attributes will be
   returned as a set"
  [db e attr if-not]
  (let [result (d/q '[:find ?v
                      :in $ ?e ?a
                      :where [?e ?a ?v]]
                    db e (d/entid db attr))]
    (if (seq result)
      (case (schema/cardinality db attr)
            :db.cardinality/one (ffirst result)
            :db.cardinality/many (into #{} (map first result)))
      if-not)))

(defn modes
  "Returns the set of modes."
  [coll]
  (->> (frequencies coll)
       (reduce
        (fn [[modes ct] [k v]]
          (cond
           (< v ct)  [modes ct]
           (= v ct)  [(conj modes k) ct]
           (> v ct) [#{k} v]))
        [#{} 2])
       first))
