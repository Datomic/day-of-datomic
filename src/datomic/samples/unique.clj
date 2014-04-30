;   Copyright (c) Metadata Partners, LLC. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns datomic.samples.unique
  (:require [clojure.set :as set]
            [datomic.api :as d]))

(defn existing-values
  "Returns subset of values that already exist as unique
   attribute attr in db"
  [db attr vals]
  (->> (d/q '[:find ?val
              :in $ ?attr [?val ...]
              :where [_ ?attr ?val]]
            db (d/entid db attr) vals)
       (map first)
       (into #{})))

(defn assert-new-values
  "Assert emaps whose attr value does not already exist in db.

   Returns transaction result or nil if nothing to assert."
  [conn part attr emaps]
  (let [vals (mapv attr emaps)
        existing (existing-values (d/db conn) attr vals)]
    (when-not (= (count existing) (count vals))
      (->> emaps
           (remove #(existing (get attr %)))
           (map (fn [emap] (assoc emap :db/id (d/tempid part))))
           (d/transact conn)
           deref))))


