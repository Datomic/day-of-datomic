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

(def db (d/db conn))

;; Partitions are NOT designed to be used like an attribute, i.e. to
;; be queried over.  They are merely a storage organization hint,
;; which can improve the performance of queries when used
;; appropriately.

;; If you MUST find all entities in a partition, use something like the following:

(->> (d/seek-datoms db :eavt (d/entid-at db :db.part/user 0))
     (map :e)
     (take-while #(= user-partition (d/part %))))

(d/release conn)
