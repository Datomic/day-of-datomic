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

;; point in time db value
(def db (d/db conn))

(def all-stories
  (d/q '[:find [?e ...]
         :where [?e :story/url]]
       (d/db conn)))

(def new-user-id (d/tempid :db.part/user))

(def upvote-all-stories
  "Transaction data for new-user-id to upvote all stories"
  (mapv
   (fn [story] [:db/add new-user-id :user/upVotes story])
   all-stories))

(def new-user
  "Transaction data for a new user"
  [{:db/id new-user-id
    :user/email "john@example.com"
    :user/firstName "John"
    :user/lastName "Doe"}])

(def upvote-tx-result
  "In a single transaction, create new user and upvote all stories"
  (->> (concat upvote-all-stories new-user)
       (d/transact conn)
       deref))

(def change-user-name-result
  "Demonstrates upsert. Tempid will resolve to existing id to
   match specified :user/email."
  @(d/transact
    conn
    [{:user/email "john@example.com" ;; this finds the existing entity
      :db/id #db/id [:db.part/user]  ;; will be replaced by existing id
      :user/firstName "Johnathan"}]))

(def john [:user/email "john@example.com"])

(def johns-upvote-for-pg
  (d/q '[:find ?story .
         :in $ ?e
         :where [?e :user/upVotes ?story]
         [?story :story/url "http://www.paulgraham.com/avg.html"]]
       (d/db conn)
       john))

(def db (:db-after @(d/transact
                     conn
                     [[:db/retract john :user/upVotes johns-upvote-for-pg]])))

;; should now be only two, since one was retracted
(:user/upVotes (d/entity db john))

(def data-that-retracts-johns-upvotes
  (let [db (d/db conn)]
    (->> (d/q '[:find ?op ?e ?a ?v
                :in $ ?op ?e ?a
                :where [?e ?a ?v]]
              db
              :db/retract
              john
              :user/upVotes)
         (into []))))

(def db (:db-after @(d/transact conn data-that-retracts-johns-upvotes)))

;; all gone
(:user/upVotes (d/entity db john))

(doc repl/gen-users-with-upvotes)

(def ten-new-users
  (repl/gen-users-with-upvotes all-stories "user" 10))

(def db (:db-after @(d/transact conn ten-new-users)))

;; how many users are there? 
(d/q '[:find (count ?e) .
       :where [?e :user/email ?v]]
     db)

;; how many users have upvoted something?
(d/q '[:find (count ?e) .
       :where [?e :user/email]
       [?e :user/upVotes]]
     db)

;; Datomic does not need a left join to keep entities missing
;; some attribute. Just leave that attribute out of the :where,
;; and then pull it during the :find.
(def users-with-upvotes
  (d/q '[:find [(pull ?e [:user/email {:user/upVotes [:story/url]}]) ...]
         :where [?e :user/email]] db))
