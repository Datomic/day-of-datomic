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
(import datomic.Datom)

(def conn (repl/scratch-conn))
(repl/transact-all conn (repl/resource "day-of-datomic/social-news.edn"))

@(d/transact conn [{:db/id (d/tempid :db.part/user)
                    :user/firstName "John"
                    :user/lastName "Doe"
                    :user/email "jdoe@example.com"
                    :user/passwordHash "<SECRET>"}])

;; plain db can see passwordHash
(def plain-db (d/db conn))
(d/q '[:find ?v . :where [_ :user/passwordHash ?v]] plain-db)
(d/touch (d/entity plain-db [:user/email "jdoe@example.com"]))
(seq (d/datoms plain-db :aevt  :user/passwordHash))

;; filtered db cannot
(def password-hash-id (d/entid plain-db :user/passwordHash))
(def password-hash-filter (fn [_ ^Datom datom] (not= password-hash-id (.a datom))))
(def filtered-db (d/filter plain-db password-hash-filter))
(d/q '[:find ?v . :where [_ :user/passwordHash ?v]] filtered-db)
(d/touch (d/entity filtered-db [:user/email "jdoe@example.com"]))
(seq (d/datoms filtered-db :aevt  :user/passwordHash))

;; filter will be called for every datom, so it is idiomatic
;; to filter only on the things that need filtering. Filtered
;; and non-filtered database values can be combined, e.g.:
(d/q '[:find (pull $filtered ?e [*])
       :in $plain $filtered ?email
       :where [$plain ?e :user/email ?email]]
     plain-db filtered-db "jdoe@example.com")

;; add a publish/at date to a transaction
@(d/transact conn [{:db/id #db/id [:db.part/user]
                    :story/title "codeq"
                    :story/url "http://blog.datomic.com/2012/10/codeq.html"}
                   {:db/id (d/tempid :db.part/tx)
                    :publish/at (java.util.Date.)}])

;; filter can reach farther than datoms, e.g. as shown below to implement
;; timescales other than Datomic's time of record. But be very mindful
;; of the performance implications of a function that is called for
;; every datom you touch!

;; all the stories
(def plain-db (d/db conn))
(d/q '[:find (count ?e) .
       :where [?e :story/url]]
     plain-db)

;; same query, filtered to stories that have been published.
(def published-db (d/filter plain-db (fn [db ^Datom datom]
                                       (-> (d/entity db (.tx datom))
                                           :publish/at))))
(d/q '[:find (count ?e) .
       :where [?e :story/url]]
     published-db)


(d/release conn)
