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

(defn partition-heads
  "Returns up to the first n datoms of each partition in the system,
   sorted by ascending e"
  [db n]
  (->> (d/q '[:find [?part ...]
             :where [:db.part/db :db.install/partition ?part]]
           db n)
      sort
      (mapcat
       (fn [part]
         (take n (d/seek-datoms db :eavt (d/entid-at db part 0)))))))

;; sample data at https://github.com/Datomic/mbrainz-sample
(def uri "datomic:dev://localhost:4334/mbrainz-1968-1973")
(def conn (d/connect uri))
(def db (d/db conn))

;; since partition is high order bits in e, entities in the
;; same partition sort togther.
(->> (partition-heads db 4) (repl/datom-table db))


