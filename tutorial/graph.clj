;; inspired by http://docs.neo4j.org/chunked/stable/cypher-cookbook-hyperedges.html

(use 'datomic.samples.repl)
(easy!)

(def conn (scratch-conn))
(transact-all conn (io/resource "day-of-datomic/graph.dtm"))

(def name-attrs [:group/name :role/name :user/name :roleInGroup/name])

(defn touch-names
  "Touch all name attributes of entities"
  [qes-result]
  (mapv
   (fn [ents]
     (mapv #(select-keys % name-attrs) ents))
   qes-result))

(defpp user1-roles-in-group2
  (touch-names
   (qes '[:find ?roleInGroup
          :where
          [?e :user/name "User1"]
          [?e :hasRoleInGroups ?roleInGroup]
          [?roleInGroup :hasGroups ?group]
          [?group :group/name "Group2"]]
        (db conn))))

(defpp all-groups-and-roles-for-user-1
  (touch-names
   (qes '[:find ?group ?role
          :where
          [?e :user/name "User1"]
          [?e :hasRoleInGroups ?roleInGroup]
          [?roleInGroup :hasGroups ?group]
          [?roleInGroup :hasRoles ?role]
          [?group :group/name]]
        (db conn))))




