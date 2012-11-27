(require
 '[clojure.java.io :as io]
 '[datomic.api :as d]
 '[datomic.samples.io :as dio]
 '[datomic.samples.query :as q])
(import datomic.Datom)

(def uri "datomic:mem://social-news")
(d/create-database uri)
(def conn (d/connect uri))

(dio/transact-all conn (io/resource "day-of-datomic/social-news.dtm"))

(d/transact conn [{:db/id (d/tempid :db.part/user)
                   :user/firstName "John"
                   :user/lastName "Doe"
                   :user/email "jdoe@example.com"
                   :user/passwordHash "<SECRET>"}])

;; plain db can see passwordHash
(def plain-db (d/db conn))
(d/q '[:find ?v :where [_ :user/passwordHash ?v]] plain-db)
(d/touch (q/find-by plain-db :user/email "jdoe@example.com"))
(seq (d/datoms plain-db :aevt  :user/passwordHash))

;; filtered db cannot
(def password-hash-id (d/entid plain-db :user/passwordHash))
(def password-hash-filter (fn [_ ^Datom datom] (not= password-hash-id (.a datom))))
(def filtered-db (d/filter (d/db conn) password-hash-filter))
(d/q '[:find ?v :where [_ :user/passwordHash ?v]] filtered-db)
(d/touch (q/find-by filtered-db :user/email "jdoe@example.com"))
(seq (d/datoms filtered-db :aevt  :user/passwordHash))

;; filter will be called for every datom, so it is idiomatic
;; to filter only on the things that need filtering. Filtered
;; and non-filtered database values can be combined, e.g.:
(-> (d/q '[:find ?ent
            :in $plain $filtered ?email
           :where [$plain ?e :user/email ?email]
           [(datomic.api/entity $filtered ?e) ?ent]]
         plain-db filtered-db "jdoe@example.com")
    ffirst d/touch)

;; add a publish/at date to a transaction
(d/transact conn [{:db/id #db/id [:db.part/user]
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
(d/q '[:find (count ?e) :where [?e :story/url]]
     plain-db)

;; same query, filtered to stories that have been published.
(def filtered-db (d/filter plain-db (fn [db ^Datom datom]
                                      (-> (d/entity db (.tx datom))
                                          :publish/at))))
(d/q '[:find (count ?e) :where [?e :story/url]]
     filtered-db)

