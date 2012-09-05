(use 'datomic.samples.repl)
(easy!)

(def conn (scratch-conn))
(transact-all conn (resource-uri "day-of-datomic/social-news.dtm"))
(transact-all conn (resource-uri "day-of-datomic/provenance.dtm"))

(def stu (qe '[:find ?e :where [?e :user/email "stuarthalloway@datomic.com"]]
             (db conn)))

(def editor (qe '[:find ?e :where [?e :user/email "editor@example.com"]]
               (db conn)))

;; Stu loves to pimp his own blog posts...
(def tx1-result (d/transact
                conn
                [{:db/id (d/tempid :db.part/user)
                  :story/title "ElastiCache in 6 minutes"
                  :story/url "http://blog.datomic.com/2012/09/elasticache-in-5-minutes.html"}
                 {:db/id (d/tempid :db.part/user)
                  :story/title "Keep Chocolate Love Atomic"
                  :story/url "http://blog.datomic.com/2012/08/atomic-chocolate.html"}
                 {:db/id (d/tempid :db.part/tx)
                  :source/user (:db/id stu)}]))

;; database t of tx1-result
(def t (d/basis-t (:db-after @tx1-result)))

;; transaction of tx1-result
(def tx (d/entity (db conn) (d/t->tx t)))

;; wall clock time of tx
(def inst (:db/txInstant tx))

;; fix spelling error in title
(d/transact
 conn
 [{:db/id (d/tempid :db.part/user)
   :story/title "ElastiCache in 5 minutes"
   :story/url "http://blog.datomic.com/2012/09/elasticache-in-5-minutes.html"}
  {:db/id (d/tempid :db.part/tx)
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
   (d/as-of (db conn) inst))

;; who changed the title, and when?
(->> (q '[:find ?e ?v ?email ?inst ?added
          :in $ ?url
          :where
          [?e :story/title ?v ?tx ?added]
          [?e :story/url ?url]
          [?tx :source/user ?user]
          [?tx :db/txInstant ?inst]
          [?user :user/email ?email]]
        (d/history (db conn))
        "http://blog.datomic.com/2012/09/elasticache-in-5-minutes.html")
     (sort-by #(nth % 3))
     pprint)



