;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require
 '[datomic.api :as d]
 '[datomic.samples.repl :as repl]
 '[datomic.samples.schema :as schema])

(def conn (repl/scratch-conn))
(def schema-map (read-string (slurp (repl/resource "day-of-datomic/schema.edn"))))
(schema/ensure-schemas conn :day-of-datomic/schema schema-map :day-of-datomic/provenance)

;; we are pretty confident about this data, so :source/confidence = 95
@(d/transact
  conn
  [{:db/id (d/tempid :db.part/user)
    :story/title "ElastiCache in 6 minutes"
    :story/url "http://blog.datomic.com/2012/09/elasticache-in-5-minutes.html"}
   {:db/id (d/tempid :db.part/tx)
    :source/confidence 95}])

;; we are less confident about this data, so :source/confidence = 40
(def db (:db-after @(d/transact
                     conn
                     [{:db/id (d/tempid :db.part/user)
                       :story/title "Request for Urgent Business Relationship"
                       :story/url "http://example.com/bogus-url"}
                      {:db/id (d/tempid :db.part/tx)
                       :source/confidence 40}])))

;; all the titles
(d/q '[:find [?title ...]
       :where [_ :story/title ?title]]
     db)

;; stories we are 90% confident in, by query
(d/q '[:find [?title ...]
       :where
       [_ :story/title ?title ?tx]
       [?tx :source/confidence ?conf]
       [(<= 90 ?conf)]]
     db)

(defn with-confidence-level
  "Filter database to only those datoms whose
   transaction :source/confidence is <= level.  Sources
   whose confidence is not specified are presumed not trusted."
  [db level]
  (d/filter
   db
   (fn [db ^datomic.Datom datom]
     (let [conf (-> (d/entity db (.tx datom))
                    :source/confidence)]
       (and conf (<= level conf ))))))

;; stories we are 90% confident in, by filter
(d/q '[:find [?title ...]
       :where
       [_ :story/title ?title ?tx]]
     (with-confidence-level db 90))





