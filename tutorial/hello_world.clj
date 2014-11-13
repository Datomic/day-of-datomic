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

(doc repl/scratch-conn)

(def conn (repl/scratch-conn))

;; transaction input and result are data
@(d/transact
  conn
  [[:db/add
    (d/tempid :db.part/user)
    :db/doc
    "Hello world"]])

;; point in time db value
(def db (d/db conn))

;; query input and result are data
(def q-result (d/q '[:find ?e .
                    :where [?e :db/doc "Hello world"]]
                  db))

;; entity is a navigable view over data
(def ent (d/entity db q-result))

;; entities are lazy, so...
(d/touch ent)

;; schema itself is data
(def doc-ent (d/entity db :db/doc))

(d/touch doc-ent)
