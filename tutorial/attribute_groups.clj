;; You can use rules to develop queries that match the same criterion
;; against a group of attributes
(use 'datomic.samples.repl)
(easy!)
(def conn (scratch-conn))

(transact-all conn (io/resource "day-of-datomic/social-news.dtm"))

;; find all attributes in the story namespace
(d/q '[:find ?e
       :in $
       :where
       [?e :db/valueType]
       [?e :db/ident ?a]
       [(namespace ?a) ?ns]
       [(= ?ns "story")]]
     (d/db conn))

;; create a reusable rule
(def rules
  '[[[attr-in-namespace ?e ?ns2]
     [?e :db/ident ?a]
     [?e :db/valueType]
     [(namespace ?a) ?ns1]
     [(= ?ns1 ?ns2)]]])

;; find all attributes in story namespace, using the rule
(d/q '[:find ?e
       :in $ %
       :where
       (attr-in-namespace ?e "story")]
     (d/db conn) rules)

;; find all entities possessing *any* story attribute
(d/q '[:find ?e
     :in $ %
       :where
       (attr-in-namespace ?a "story")
       [?e ?a]]
     (d/db conn) rules)


