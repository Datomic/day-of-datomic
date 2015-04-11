;   Copyright (c) Cognitect. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require
 '[clojure.pprint :as pp]
 '[datomic.api :as d])

(set! *print-length* 25)

;; entity-at returns time at which ?e appears in entity position
;; value-at returns time at which ?e appears in value position
;; entites-with returns other entites appearing in entity position in same tx as ?e
(def time-rules
  '[[(entity-at [?e] ?tx ?t ?inst)
     [?e _ _ ?tx]
     [(datomic.api/tx->t ?tx) ?t]
     [?tx :db/txInstant ?inst]]
    [(value-at [?e] ?tx ?t ?inst)
     [_ _ ?e ?tx]
     [(datomic.api/tx->t ?tx) ?t]
     [?tx :db/txInstant ?inst]]
    [(entities-with [?log ?e] ?es)
     [?e _ _ ?tx]
     [(tx-data ?log ?tx) [[?es]]]]])

;; Example usage of time-rules against https://github.com/Datomic/mbrainz-sample
(def conn (d/connect "datomic:dev://localhost:4334/mbrainz-1968-1973"))
(def db (d/db conn))

;; add something to John Lennon right now
@(d/transact
  conn
  [[:db/add
    (d/q '[:find ?e . :where [?e :artist/name "John Lennon"]] db)
    :db/doc
    "Silly extra fact about John"]])

(def hist (d/history (d/db conn)))

;; created-at is min of entity-at
(d/q '[:find (min ?t) (min ?inst)
       :in $ % ?n
       :where
       [?e :artist/name ?n]
       (entity-at ?e ?tx ?t ?inst)]
     hist time-rules "John Lennon")

;; updated-at is max of entity-at
(d/q '[:find (max ?t) (max ?inst)
       :in $ % ?n
       :where
       [?e :artist/name ?n]
       (entity-at ?e ?tx ?t ?inst)]
     hist time-rules "John Lennon")

;; first-referenced-at is min of object-at
;; last-referenced-at similar and not shown
;; (exercise to reader: build this with SQL + convention)
(d/q '[:find (max ?t) (max ?inst)
       :in $ % ?n
       :where
       [?e :artist/name ?n]
       (value-at ?e ?tx ?t ?inst)]
     hist time-rules "John Lennon")

;; artists-with: other artists appearing in same transaction with John
;; (exercise to reader: build this with SQL + convention)
(d/q '[:find ?names
       :in $ % ?log ?from-name
       :where
       [?e :artist/name ?from-name]
       (entities-with ?log ?e ?es)
       [?es :artist/name ?names]]
     hist time-rules (d/log conn) "John Lennon")

;; helper tn
(defn tableize
  "Print tuples as an ASCII table with ks as headings"
  [ks tuples]
  (->> tuples
       (map (partial zipmap ks))
       (pp/print-table ks)))

;; mentioned-at is either entity-at and object-at
;; (exercise to reader: build this with SQL + convention)
(->> (d/q '[:find ?t ?inst
            :in $ % ?n
            :where
            [?e :artist/name ?n]
            (or (entity-at ?e ?tx ?t ?inst)
                (value-at ?e ?tx ?t ?inst))]
          hist time-rules "John Lennon")
     (sort-by first)
     (tableize [:t :inst]))

;; hmm, when did we touch production schema
;; (exercise to reader: build this with SQL + convention)
(->> (d/q '[:find ?a ?ident ?t ?inst
            :in $ %
            :where
            (or [:db.part/db :db.install/attribute ?a]
                [:db.part/db :db.alter/attribute ?a])
            [(datomic.api/ident $ ?a) ?ident]
            (or (entity-at ?a ?tx ?t ?inst)
                (value-at ?a ?tx ?t ?inst))]
          hist time-rules)
     (sort-by #(nth % 2))
     (tableize [:a :ident :t :inst]))






