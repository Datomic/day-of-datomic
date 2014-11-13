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

;; The examples below parallel http://docs.datomic.com/query.html#find-specifications
(def conn (repl/scratch-conn))
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

(d/release conn)
