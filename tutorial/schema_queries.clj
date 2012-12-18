;; work through at the REPL, evaulating each form
(use 'datomic.samples.repl)
(easy!)

(def conn (scratch-conn))
(transact-all conn (io/resource "day-of-datomic/social-news.edn"))
(def db (d/db conn))

;; find the idents of all schema elements in the system
(d/q '[:find ?ident
       :where [_ :db/ident ?ident]]
     db)

;; find just the attributes
(d/q '[:find ?ident
       :where
       [?e :db/ident ?ident]
       [_ :db.install/attribute ?e]]
     db)

;; find just the data functions
(d/q '[:find ?ident
       :where
       [?e :db/ident ?ident]
       [_ :db.install/function ?e]]
     db)

;; documentation of a schema element
(-> db (d/entity :db.unique/identity) :db/doc)

;; complete details of a schema element
(-> db (d/entity :user/email) d/touch)

;; find attributes with AVET index
(d/q '[:find ?ident
       :where
       [?e :db/ident ?ident]
       [?e :db/index true]
       [_ :db.install/attribute ?e]]
     db)

;; find attributes in the user namespace
(d/q '[:find ?ident
       :where
       [?e :db/ident ?ident]
       [_ :db.install/attribute ?e]
       [(namespace ?ident) ?ns]
       [(= ?ns "user")]]
     db)

;; find all reference attributes
(d/q '[:find ?ident
       :where
       [?e :db/ident ?ident]
       [_ :db.install/attribute ?e]
       [?e :db/valueType :db.type/ref]]
     db)

;; find all attributes that are cardinality-many
(d/q '[:find ?ident
       :where
       [?e :db/ident ?ident]
       [_ :db.install/attribute ?e]
       [?e :db/cardinality :db.cardinality/many]]
     db)



