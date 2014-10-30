(require '[datomic.api :as d])

(def uri "datomic:mem://transaction-function-example")
;; (def uri "datomic:dev://localhost:4334/transaction-function-example")
(d/create-database uri)
(def conn (d/connect uri))

;; install a transaction function
(d/transact conn [ {:db/id #db/id [:db.part/user]
                    :db/ident :examples/ensure-composite
                    :db/doc "Create an entity with k1=v1, k2=v2, throwing if such an entity already exists"
                    :db/fn (d/function
                            '{:lang "clojure"
                              :params [db k1 v1 k2 v2]
                              :code (if-let [[e t1 t2] (d/q '[:find [?e ?t1 ?t2]
                                                              :in $ ?k1 ?v1 ?k2 ?v2
                                                              :where
                                                              [?e ?k1 ?v1 ?t1]
                                                              [?e ?k2 ?v2 ?t2]]
                                                            db k1 v1 k2 v2)]
                                      (throw (ex-info (str "Entity already exists " e)
                                                      {:e e :t (d/tx->t (max t1 t2))}))
                                      [{:db/id (d/tempid :db.part/user)
                                        k1 v1
                                        k2 v2}])})}])

;; test locally
(d/invoke (d/db conn) :examples/ensure-composite (d/db conn) :db/ident :example/object :db/doc "Example object" )

;; first ensure wins
@(d/transact conn [[:examples/ensure-composite :db/ident :example/object :db/doc "Example object"]])

;; second ensure throws exception including t at which entity is known
@(d/transact conn [[:examples/ensure-composite :db/ident :example/object :db/doc "Example object"]])

;; coordination: wait until this peer knows a t where e had k1->v1, k2->v2
(def t (:t (ex-data (.getCause *e))))

;; wait to be sure we know about the entity
;; trivial in this case, but intesting if :examples/ensure-composite called from different peers
@(d/sync conn t)

