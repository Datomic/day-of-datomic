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

;; some St*rts
@(d/transact conn [{:db/id #db/id [:db.part/user]
                    :user/firstName "Stewart"
                    :user/lastName "Brand"}
                   {:db/id #db/id [:db.part/user]
                    :user/firstName "John"
                    :user/lastName "Stewart"}
                   {:db/id #db/id [:db.part/user]
                    :user/firstName "Stuart"
                    :user/lastName "Smalley"}
                   {:db/id #db/id [:db.part/user]
                    :user/firstName "Stuart"
                    :user/lastName "Halloway"}])

;; database point-in-time value
(def db (d/db conn))

;; find all the Stewart first names
(d/q '[:find [?e ...]
       :in $ ?name
       :where [?e :user/firstName ?name]]
     db
     "Stewart")

;; find all the Stewart or Stuart first names
(d/q '[:find [?e ...]
     :in $ [?name ...]
     :where [?e :user/firstName ?name]]
   db
   ["Stewart" "Stuart"])

;; find all the Stewart/Stuart as either first name or last name
(d/q '[:find [?e ...]
       :in $ [?name ...] [?attr ...]
       :where [?e ?attr ?name]]
     db
     ["Stewart" "Stuart"]
     [:user/firstName :user/lastName])

;; find only the Smalley Stuarts
(d/q '[:find [?e ...]
       :in $ ?fname ?lname
       :where [?e :user/firstName ?fname]
              [?e :user/lastName ?lname]]
     db
     "Stuart"
     "Smalley")

;; same query as above, but with map form
(d/q '{:find [[?e ...]]
       :in [$ ?fname ?lname]
       :where [[?e :user/firstName ?fname]
               [?e :user/lastName ?lname]]}
     db
     "Stuart"
     "Smalley")

(d/release conn)

