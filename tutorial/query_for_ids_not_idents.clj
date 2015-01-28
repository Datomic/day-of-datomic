;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require '[datomic.api :as d])
(set! *print-length* 10)

(def uri "datomic:mem://test")
(d/create-database uri)
(def conn (d/connect uri))
@(d/transact-async conn [{:db/id (d/tempid :db.part/user)
                          :db/ident :color/green}
                         {:db/id (d/tempid :db.part/user)
                          :db/ident :color/red}
                         {:db/id (d/tempid :db.part/user)
                          :db/ident :color/blue}
                         {:db/id (d/tempid :db.part/db)
                          :db/ident :item/color
                          :db/cardinality :db.cardinality/one
                          :db/valueType :db.type/ref
                          :db.install/_attribute :db.part/db}])

;; create a million items, colored at random (takes a few seconds)
@(d/transact
  conn
  (repeatedly 1000000 (fn [] [:db/add (d/tempid :db.part/user) :item/color
                              (rand-nth [:color/green :color/red :color/blue])])))

(def db (d/db conn))

;; query directly for id: fast
(dotimes [_ 10]
  (time (d/q '[:find (count ?item) .
               :where [?item :item/color :color/green]]
             db)))

;; add unnecessary indirection to ident: slower
(dotimes [_ 10]
  (time (d/q '[:find (count ?item) .
               :where [?item :item/color ?color]
                      [?color :db/ident :color/green]]
             db)))
