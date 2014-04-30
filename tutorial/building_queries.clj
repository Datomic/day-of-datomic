(use 'datomic.samples.repl)
(easy!)

(def conn (scratch-conn))
(transact-all conn (io/resource "day-of-datomic/social-news.edn"))

;; some St*rts
(d/transact conn [{:db/id #db/id [:db.part/user]
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

;; find all the Stewart first names
(d/q '[:find ?e
     :in $ ?name
     :where [?e :user/firstName ?name]]
   (d/db conn)
   "Stewart")

;; find all the Stewart or Stuart first names
(d/q '[:find ?e
     :in $ [?name ...]
     :where [?e :user/firstName ?name]]
   (d/db conn)
   ["Stewart" "Stuart"])

;; find all the Stewart/Stuart as either first name or last name
(let [db (d/db conn)]
  (d/q '[:find ?e
         :in $ [?name ...] [?attr ...]
         :where [?e ?attr ?name]]
       db
       ["Stewart" "Stuart"]
       [(d/entid db :user/firstName) (d/entid db :user/lastName)]))

;; find only the Smalley Stuarts
(d/q '[:find ?e
       :in $ ?fname ?lname
       :where [?e :user/firstName ?fname]
              [?e :user/lastName ?lname]]
     (d/db conn)
     "Stuart"
     "Smalley")

;; same query above, but with map form
(d/q '{:find [?e]
       :in [$ ?fname ?lname]
       :where [[?e :user/firstName ?fname]
               [?e :user/lastName ?lname]]}
     (d/db conn)
     "Stuart"
     "Smalley")


