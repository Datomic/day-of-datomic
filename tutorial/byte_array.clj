(use 'datomic.samples.repl)
(easy!)

(def conn (scratch-conn))

;; Load simple schema
@(d/transact conn [{:db/id                 #db/id[:db.part/db]
                    :db/ident              :some-bytes
                    :db/valueType          :db.type/bytes
                    :db/cardinality        :db.cardinality/one
                    :db.install/_attribute :db.part/db}])

(def bytes-1 (byte-array (map byte [1 2 3])))
(def bytes-2 (byte-array (map byte [1 2 3])))

@(d/transact conn [{:db/id      (d/tempid :db.part/user)
                    :some-bytes bytes-1}
                   {:db/id      (d/tempid :db.part/user)
                    :some-bytes bytes-2}])

(= bytes-1 bytes-2)
;;=> false

;; therefore...

(d/q '[:find ?e
     :in $ ?bytes
     :where [?e :some-bytes ?bytes]]
   (d/db conn)
   (byte-array (map byte [1 2 3])))

;;=> #{}

;;=> Use java.util.Arrays/equals instead

(d/q '[:find ?e
     :in $ ?bytes
     :where [?e :some-bytes ?sbytes]
     [(java.util.Arrays/equals ^bytes ?sbytes ^bytes ?bytes)]]
   (d/db conn)
   (byte-array (map byte [1 2 3])))

;;=> #{[17592186045418] [17592186045419]}
