(use '[datomic.api :only (q db) :as d])
(def uri "datomic:mem://hello")
(d/create-database uri)
(def conn (d/connect uri))

;; transaction input is data
(def tx-result
  (d/transact
   conn
   [[:db/add
     (d/tempid :db.part/user)
     :db/doc
     "Hello world"]]))

;; transaction result is data
tx-result

(def dbval (db conn))

;; query input is data
(def q-result (q '[:find ?e
                   :where [?e :db/doc "Hello world"]]
                 dbval))

;; query result is data
q-result

;; entity is a navigable view over data
(def ent (d/entity dbval (ffirst q-result)))

;; entities are lazy, so...
(d/touch ent)

;; schema itself is data
(def doc-ent (d/entity dbval :db/doc))

(d/touch doc-ent)
