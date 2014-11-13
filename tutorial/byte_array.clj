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

;; Load simple schema
@(d/transact conn [{:db/id                 #db/id[:db.part/db]
                    :db/ident              :some-bytes
                    :db/valueType          :db.type/bytes
                    :db/cardinality        :db.cardinality/one
                    :db.install/_attribute :db.part/db}])

(def bytes-1 (byte-array (map byte [1 2 3])))
(def bytes-2 (byte-array (map byte [1 2 3])))

@(d/transact conn [{:db/id (d/tempid :db.part/user)
                    :some-bytes bytes-1}
                   {:db/id (d/tempid :db.part/user)
                    :some-bytes bytes-2}])

;; point-in-time db value
(def db (d/db conn))

;; Java does not think equal byte arrays equal
(= bytes-1 bytes-2)
;;=> false

;; this will not work as desired
(d/q '[:find [?e ...]
     :in $ ?bytes
     :where [?e :some-bytes ?bytes]]
   db
   (byte-array (map byte [1 2 3])))
;;=> #{}

;; Use java.util.Arrays/equals instead
(d/q '[:find [?e ...]
     :in $ ?bytes
       :where [?e :some-bytes ?sbytes]
       [(java.util.Arrays/equals ^bytes ?sbytes ^bytes ?bytes)]]
     (d/db conn)
     (byte-array (map byte [1 2 3])))
;;=> [17592186045418 17592186045419]

(d/release conn)
