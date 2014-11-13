;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require
 '[clojure.pprint :as pp]
 '[datomic.api :as d]
 '[datomic.samples.repl :as repl])

(def conn (repl/scratch-conn))

(def txes
  [[{:db/id #db/id[:db.part/db]
     :db/ident :item/id
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db/unique :db.unique/identity
     :db.install/_attribute :db.part/db}
    {:db/id #db/id[:db.part/db]
     :db/ident :item/description
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db/fulltext true
     :db.install/_attribute :db.part/db}
    {:db/id #db/id[:db.part/db]
     :db/ident :item/count
     :db/valueType :db.type/long
     :db/cardinality :db.cardinality/one
     :db/index true
     :db.install/_attribute :db.part/db}
    {:db/id #db/id[:db.part/db]
     :db/ident :tx/error
     :db/valueType :db.type/boolean
     :db/index true
     :db/cardinality :db.cardinality/one
     :db.install/_attribute :db.part/db}
    {:db/id #db/id [:db.part/tx]
     :db/txInstant #inst "2012"}]
   [{:db/id #db/id [:db.part/user]
     :item/id "DLC-042"
     :item/description "Dilitihium Crystals"
     :item/count 100}
    {:db/id #db/id [:db.part/tx]
     :db/txInstant #inst "2013-01"}]
   [{:db/id [:item/id "DLC-042"]
     :item/count 250}
    {:db/id #db/id [:db.part/tx]
     :db/txInstant #inst "2013-02"}]
   [{:db/id [:item/id "DLC-042"]
     :item/count 50}
    {:db/id #db/id [:db.part/tx]
     :db/txInstant #inst "2014-02-28"}]
   [{:db/id [:item/id "DLC-042"]
     :item/count 9999}
    {:db/id #db/id [:db.part/tx]
     :db/txInstant #inst "2014-04-01"
     :tx/error true}]
   [{:db/id [:item/id "DLC-042"]
     :item/count 100}
    {:db/id #db/id [:db.part/tx]
     :db/txInstant #inst "2014-05-15"}]])

(doseq [tx txes]
  @(d/transact conn tx))

(def db (d/db conn))
(def as-of-eoy-2013 (d/as-of db #inst "2014-01-01"))
(def since-2014 (d/since db #inst "2014-01-01"))
(def history (d/history db))

(def error-txes (set (d/q '[:find [?e ...]
                            :where [?e :tx/error]]
                          db)))

(defn correct?
  [_ datom]
  (not (contains? error-txes (:tx datom))))

(def corrected (d/filter history correct?))

(d/touch (d/entity db [:item/id "DLC-042"]))
(d/touch (d/entity as-of-eoy-2013 [:item/id "DLC-042"]))


;; common mistake with since:
;; looking up something by key that is not in the time window
(d/entity since-2014 [:item/id "DLC-042"])

;; solution: lookup with current db, then use since for the entity
(d/touch (d/entity since-2014 (d/entid db [:item/id "DLC-042"])))

;; more likely: multi-point-in-time join
(d/q '[:find ?count .
       :in $ $since ?id
       :where [$ ?e :item/id ?id]
              [$since ?e :item/count ?count]]
     db since-2014 "DLC-042")

;; full history of dilithium crystal assertions
(->> (d/q '[:find ?aname ?v ?inst
            :in $ ?e
            :where [?e ?a ?v ?tx true]
                   [?tx :db/txInstant ?inst]
                   [?a :db/ident ?aname]]
          history [:item/id "DLC-042"])
     (sort-by #(nth % 2))
     pp/pprint)

;; full history of dilithium crystal counts
(->> (d/q '[:find ?inst ?count
            :in $ ?id
            :where [?id :item/count ?count ?tx true]
            [?tx :db/txInstant ?inst]]
          history [:item/id "DLC-042"])
     (sort-by first)
     pp/pprint)

;; corrected history of dilithium crystal counts
(->> (d/q '[:find ?inst ?count
            :in $ ?id
            :where [?id :item/count ?count ?tx true]
            [?tx :db/txInstant ?inst]]
          corrected [:item/id "DLC-042"])
     (sort-by first)
     pp/pprint)

(d/release conn)














