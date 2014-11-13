;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require
 '[datomic.api :as d]
 '[datomic.samples.repl :as repl])

(def conn (repl/scratch-conn))
(repl/transact-all conn (repl/resource "day-of-datomic/social-news.edn"))
(repl/transact-all conn (repl/resource "day-of-datomic/provenance.edn"))

(def stu [:user/email "stuarthalloway@datomic.com"])

;; Stu loves to pimp his own blog posts...
(def db (:db-after @(d/transact
                     conn
                     [{:db/id (d/tempid :db.part/user)
                       :story/title "ElastiCache in 6 minutes"
                       :story/url "http://blog.datomic.com/2012/09/elasticache-in-5-minutes.html"}
                      {:db/id (d/tempid :db.part/user)
                       :story/title "Keep Chocolate Love Atomic"
                       :story/url "http://blog.datomic.com/2012/08/atomic-chocolate.html"}
                      {:db/id (d/tempid :db.part/tx)
                       :source/user stu}])))

;; database t of tx1-result
(def t (d/basis-t db))

;; transaction of tx1-result
(def tx (d/t->tx t))

;; wall clock time of tx
(:db/txInstant (d/entity db tx))

(def editor [:user/email "editor@example.com"])

;; fix spelling error in title
;; note auto-upsert and attribution
(def db (:db-after @(d/transact
                     conn
                     [{:db/id (d/tempid :db.part/user)
                       :story/title "ElastiCache in 5 minutes"
                       :story/url "http://blog.datomic.com/2012/09/elasticache-in-5-minutes.html"}
                      {:db/id (d/tempid :db.part/tx)
                       :source/user editor}])))

(def story [:story/url "http://blog.datomic.com/2012/09/elasticache-in-5-minutes.html"])

;; what is the title now?
(:story/title (d/entity db story))

;; what was the title as of earlier point in time?
(:story/title (d/entity (d/as-of db tx) story))

;; who changed the title, and when?
(->> (d/q '[:find ?e ?v ?email ?inst ?added
            :in $ ?e
            :where
            [?e :story/title ?v ?tx ?added]
            [?tx :source/user ?user]
            [?tx :db/txInstant ?inst]
            [?user :user/email ?email]]
          (d/history (d/db conn))
          story)
     (sort-by #(nth % 3))
     pprint)

;; what is the entire history of entity e?
(->> (d/q '[:find ?aname ?v ?tx ?inst ?added
            :in $ ?e
            :where
            [?e ?a ?v ?tx ?added]
            [?a :db/ident ?aname]
            [?tx :db/txInstant ?inst]]
          (d/history (d/db conn))
          story)
     seq
     (sort-by #(nth % 2))
     pprint)
