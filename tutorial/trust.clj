(use 'datomic.samples.repl)
(easy!)
(def conn (scratch-conn))

(def schema-map (read-string (slurp (io/resource "day-of-datomic/schema.edn"))))
(ensure-schemas conn :day-of-datomic/schema schema-map :day-of-datomic/provenance)

;; we are pretty confident about this data, so :source/confidence = 95
@(d/transact
  conn
  [{:db/id (d/tempid :db.part/user)
    :story/title "ElastiCache in 6 minutes"
    :story/url "http://blog.datomic.com/2012/09/elasticache-in-5-minutes.html"}
   {:db/id (d/tempid :db.part/tx)
    :source/confidence 95}])

;; we are less confident about this data, so :source/confidence = 40
@(d/transact
  conn
  [{:db/id (d/tempid :db.part/user)
    :story/title "Request for Urgnent Business Relationship"
    :story/url "http://example.com/bogus-url"}
   {:db/id (d/tempid :db.part/tx)
    :source/confidence 40}])

;; all the stories
(d/q '[:find ?title
       :where [_ :story/title ?title]]
     (d/db conn))

;; stories we are 90% confident in, by query
(d/q '[:find ?title
       :where
       [_ :story/title ?title ?tx]
       [?tx :source/confidence ?conf]
       [(<= 90 ?conf)]]
     (d/db conn))

(defn with-confidence-level
  "Filter database to only those datoms whose
   transaction :source/confidence is <= level.  Sources
   whose confidence is not specified are presumed trusted."
  [db level]
  (d/filter
   db
   (fn [db ^datomic.Datom datom]
     (let [conf (-> (d/entity db (.tx datom))
                    :source/confidence)]
       (and conf (<= level conf ))))))

;; stories we are 90% confident in, by filter
(d/q '[:find ?title
       :where
       [_ :story/title ?title ?tx]]
     (-> (d/db conn) (with-confidence-level 90)))





