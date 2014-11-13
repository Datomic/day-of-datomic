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

(repl/transact-all conn (repl/resource "day-of-datomic/bigger-than-pluto.edn"))
(def db (d/db conn))

;; how many objects are there?
(d/q '[:find (count ?e) .
       :where [?e :object/name ?n]]
     db)

;; largest radius?
(d/q '[:find (max ?radius) .
       :where [_ :object/meanRadius ?radius]]
     db)

;; smallest radius
(d/q '[:find (min ?radius) .
       :where [_ :object/meanRadius ?radius]]
     db)

;; average radius
(d/q '[:find (avg ?radius) .
       :with ?e
       :where [?e :object/meanRadius ?radius]]
     db)

;; median radius
(d/q '[:find (median ?radius) .
       :with ?e
       :where [?e :object/meanRadius ?radius]]
     db)

;; stddev
(d/q '[:find (stddev ?radius) .
       :with ?e
       :where [?e :object/meanRadius ?radius]]
     db)

;; random solar system object
(d/q '[:find (rand ?name) .
       :where [?e :object/name ?name]]
     db)

;; smallest 3
(d/q '[:find (min 3 ?radius) .
       :with ?e
       :where [?e :object/meanRadius ?radius]]
     db)

;; largest 3
(d/q '[:find (max 3 ?radius) .
       :with ?e
       :where [?e :object/meanRadius ?radius]]
     db)

;; 5 random (duplicates possible)
(d/q '[:find (rand 5 ?name) .
       :with ?e
       :where [?e :object/name ?name]]
     db)

;; choose 5, no duplicates
(d/q '[:find (sample 5 ?name) .
       :with ?e
       :where [?e :object/name ?name]]
     db)

;; what is the average length of a
;; schema name?
(d/q '[:find (avg ?length) .
       :with ?e
       :where
       [?e :db/ident ?ident]
       [(name ?ident) ?name]
       [(count ?name) ?length]]
     db)

;; ... and the mode(s)?
(d/q '[:find (datomic.samples.repl/modes ?length) .
       :with ?e
       :where
       [?e :db/ident ?ident]
       [(name ?ident) ?name]
       [(count ?name) ?length]]
     db)

;; how many attributes and value types does this
;; schema use?
(d/q '[:find  (count ?a) (count-distinct ?vt)
       :where
       [?a :db/ident ?ident]
       [?a :db/valueType ?vt]]
     db)

(d/release conn)






