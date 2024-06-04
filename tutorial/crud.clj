;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require '[datomic.api :as d])

;; new database
(def uri "datomic:mem://localhost:4334/crud-example")
(d/create-database uri)
(def conn (d/connect uri))

;; attribute schema for :crud/name
@(d/transact
  conn
  [{:db/id (d/tempid :db.part/db)
    :db/ident :crud/name
    :db/valueType :db.type/string
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}])

;; create, awaiting point-in-time-value
(def db-after-create
  (-> (d/transact
       conn
       [[:db/add (d/tempid :db.part/user) :crud/name "Hello world"]])
      deref
      :db-after))

;; read
(d/pull db-after-create '[*] [:crud/name "Hello world"])

;; update
(-> (d/transact
     conn
     [[:db/add [:crud/name "Hello world"]
       :db/doc "An entity with only demonstration value"]])
    deref
    :db-after
    (d/pull '[*] [:crud/name "Hello world"]))

;; "delete" adds new information, does not erase old
(def db-after-delete
  (-> (d/transact conn
                  [[:db.fn/retractEntity [:crud/name "Hello world"]]])
      deref
      :db-after))

;; no present value for deleted entity
(d/pull db-after-delete '[*] [:crud/name "Hello world"])

;; but everything ever said is still there
(def history (d/history db-after-delete))
(require '[clojure.pprint :as pp])
(->> (d/q '[:find ?e ?a ?v ?tx ?op
            :in $
            :where [?e :crud/name "Hello world"]
                   [?e ?a ?v ?tx ?op]]
          history)
     (map #(zipmap [:e :a :v :tx :op] %))
     (sort-by :tx)
     (pp/print-table [:e :a :v :tx :op]))

