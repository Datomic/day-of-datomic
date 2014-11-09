;; inspired by http://docs.neo4j.org/chunked/stable/cypher-cookbook-hyperedges.html

(use 'datomic.samples.repl)
(easy!)

(def conn (scratch-conn))
(transact-all conn (io/resource "day-of-datomic/graph.edn"))

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




