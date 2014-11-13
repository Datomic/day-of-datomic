;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require
 '[datomic.api :as d]
 '[datomic.samples.repl :as repl])

(def conn (repl/scratch-conn))

(def ensure-composite
  (d/function
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
               k2 v2}])}))
;; install a transaction function
@(d/transact conn [{:db/id #db/id [:db.part/user]
                    :db/ident :examples/ensure-composite
                    :db/doc "Create an entity with k1=v1, k2=v2, throwing if such an entity already exists"
                    :db/fn ensure-composite}])

(def db (d/db conn))

;; test locally
(d/invoke db :examples/ensure-composite db
          :db/ident :example/object :db/doc "Example object" )

(def tx-data [[:examples/ensure-composite :db/ident :example/object :db/doc "Example object"]])
;; first ensure wins
@(d/transact conn tx-data)

;; second ensure throws exception including t at which entity is known
(try
 @(d/transact conn tx-data)
 (catch Exception e
   (println "Got expected exception " (.getMessage e))
   (def t (:t (ex-data (.getCause e))))))

;; wait to be sure we know about the entity
;; trivial in this case, but intesting if :examples/ensure-composite called from different peers
@(d/sync conn t)

