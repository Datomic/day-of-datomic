(use 'datomic.samples.repl)
(easy!)

(def conn (scratch-conn))

(def user-partition (d/entid (d/db conn) :db.part/user))

@(d/transact conn [{:db/id                 #db/id[:db.part/db]
                    :db/ident              :an-attribute
                    :db/valueType          :db.type/keyword
                    :db/cardinality        :db.cardinality/one
                    :db.install/_attribute :db.part/db}])

@(d/transact conn [{:db/id        (d/tempid :db.part/user)
                    :an-attribute :foo}
                   {:db/id        (d/tempid :db.part/user)
                    :an-attribute :bar}])

;; Partitions are NOT designed to be used like an attribute, i.e. to
;; be queried over.  They are merely a storage organization hint,
;; which can improve the performance of queries when used
;; appropriately.

;; If you MUST find all entities in a partition, use something like the following:

(->> (d/datoms (d/db conn) :eavt)
           (map :e)
           (filter (comp (partial = user-partition) d/part)))
