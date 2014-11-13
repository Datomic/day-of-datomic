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

;; inspired by http://docs.neo4j.org/chunked/stable/cypher-cookbook-hyperedges.html

(def conn (repl/scratch-conn))
(repl/transact-all conn (repl/resource "day-of-datomic/graph.edn"))

(def db (d/db conn))

(d/q '[:find (pull ?role [:role/name]) .
       :where
       [?e :user/name "User1"]
       [?e :hasRoleInGroups ?roleInGroup]
       [?roleInGroup :hasGroups ?group]
       [?group :group/name "Group2"]
       [?roleInGroup :hasRoles ?role]]
     (d/db conn))

(d/q '[:find (pull ?group [:group/name]) (pull ?role [:role/name])
       :where
       [?e :user/name "User1"]
       [?e :hasRoleInGroups ?roleInGroup]
       [?roleInGroup :hasGroups ?group]
       [?roleInGroup :hasRoles ?role]
       [?group :group/name]]
     (d/db conn))

(d/release conn)
