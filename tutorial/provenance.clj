(use 'datomic.samples.repl)
(easy!)

(def conn (scratch-conn))
(transact-all conn (io/resource "day-of-datomic/social-news.dtm"))
(transact-all conn (io/resource "day-of-datomic/provenance.dtm"))

(defpp stu (qe '[:find ?e :where [?e :user/email "stuarthalloway@datomic.com"]]
             (db conn)))

;; Stu loves to pimp his own blog posts...
(defpp tx1-result (transact
                   conn
                   [{:db/id (tempid :db.part/user)
                     :story/title "ElastiCache in 6 minutes"
                     :story/url "http://blog.datomic.com/2012/09/elasticache-in-5-minutes.html"}
                    {:db/id (tempid :db.part/user)
                     :story/title "Keep Chocolate Love Atomic"
                     :story/url "http://blog.datomic.com/2012/08/atomic-chocolate.html"}
                    {:db/id (tempid :db.part/tx)
                     :source/user (:db/id stu)}]))

;; database t of tx1-result
(defpp t (basis-t (:db-after @tx1-result)))

;; transaction of tx1-result
(defpp tx (entity (db conn) (t->tx t)))

;; wall clock time of tx
(defpp inst (:db/txInstant tx))

(defpp editor (qe '[:find ?e :where [?e :user/email "editor@example.com"]]
                  (db conn)))

;; fix spelling error in title
;; note auto-upsert and attribution
(transact
 conn
 [{:db/id (tempid :db.part/user)
   :story/title "ElastiCache in 5 minutes"
   :story/url "http://blog.datomic.com/2012/09/elasticache-in-5-minutes.html"}
  {:db/id (tempid :db.part/tx)
   :source/user (:db/id editor)}])

;; what is the title now?
(q '[:find ?v
     :where [?e :story/title ?v]
            [?e :story/url "http://blog.datomic.com/2012/09/elasticache-in-5-minutes.html"]]
   (db conn))

;; what was the title as of early point in time?
(q '[:find ?v
     :where [?e :story/title ?v]
            [?e :story/url "http://blog.datomic.com/2012/09/elasticache-in-5-minutes.html"]]
   (as-of (db conn) inst))

;; who changed the title, and when?
(->> (q '[:find ?e ?v ?email ?inst ?added
          :in $ ?url
          :where
          [?e :story/title ?v ?tx ?added]
          [?e :story/url ?url]
          [?tx :source/user ?user]
          [?tx :db/txInstant ?inst]
          [?user :user/email ?email]]
        (history (db conn))
        "http://blog.datomic.com/2012/09/elasticache-in-5-minutes.html")
     (sort-by #(nth % 3))
     pprint)



