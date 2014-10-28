;; Copyright (c) Cognitect, Inc.
;; All rights reserved.


;; The examples below parallel http://docs.datomic.com/query.html#find-specifications
(require '[datomic.api :as d])
(def uri "datomic:mem://foo")
(d/create-database uri)
(def conn (d/connect uri))
(def db (d/db conn))

;; relation find spec
(d/q '[:find ?e ?v
       :where [?e :db/ident ?v]]
     db)

;; collection find spec
(d/q '[:find [?v ...]
       :where [_ :db/ident ?v]]
     db)

;; single tuple find spec
(d/q '[:find [?e ?ident]
       :where [?e :db/ident ?ident]]
     db)

;; single scalar find spec
(d/q '[:find ?v .
       :where [0 :db/ident ?v]]
     db)
