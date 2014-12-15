;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require
 '[clojure.pprint :as pp]
 '[datomic.api :as d]
 '[datomic.samples.repl :as repl])

;; inspired by http://docs.neo4j.org/chunked/stable/cypher-cookbook-hyperedges.html

(def conn (repl/scratch-conn))
(repl/transact-all conn (repl/resource "day-of-datomic/graph.edn"))

(def db (d/db conn))

;; find roles for user and particular groups
(d/q '[:find (pull ?role [:role/name])
       :in $ ?e ?group
       :where
       [?e :hasRoleInGroups ?roleInGroup]
       [?roleInGroup :hasGroups ?group]
       [?roleInGroup :hasRoles ?role]]
     db [:user/name "User1"] [:group/name "Group2"])

;; find all groups and roles for a user
(pp/pprint
 (d/q '[:find (pull ?group [:group/name]) (pull ?role [:role/name])
        :in $ ?e
        :where
        [?e :hasRoleInGroups ?roleInGroup]
        [?roleInGroup :hasGroups ?group]
        [?roleInGroup :hasRoles ?role]]
      db [:user/name "User2"]))

(def rules '[[(user-roles-in-groups ?user ?role ?group)
              [?user :hasRoleInGroups ?roleInGroup]
              [?roleInGroup :hasGroups ?group]
              [?roleInGroup :hasRoles ?role]]])

;; find all groups and roles for a user, using a datalog rulea
(pp/pprint
 (d/q '[:find (pull ?group [:group/name]) (pull ?role [:role/name])
        :in $ % ?user
        :where (user-roles-in-groups ?user ?role ?group)]
      db rules [:user/name "User1"]))

;; find common groups based on shared roles, counting shared roles
(d/q '[:find (pull ?group [:group/name]) (count ?role)
       :in $ % ?user1 ?user2
       :where (user-roles-in-groups ?user1 ?role ?group)
              (user-roles-in-groups ?user2 ?role ?group)]
     db rules [:user/name "User1"] [:user/name "User2"])

(d/release conn)
