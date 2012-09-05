;   Copyright (c) Metadata Partners, LLC. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns datomic.samples.query
  (:use [datomic.api :only (q db) :as d]))

(defn only
  "Return the only item from a query result"
  [query-result]
  (assert (= 1 (count query-result)))
  (assert (= 1 (count (first query-result))))
  (ffirst query-result))

(defprotocol Eid
  (e [_]))

(extend-protocol Eid
  java.lang.Long
  (e [n] n)

  datomic.Entity
  (e [ent] (:db/id ent)))

(defn qe
  "Returns the single entity returned by a query."
  [query db & args]
  (let [res (apply q query db args)]
    (d/entity db (only res))))

(defn qes
  "Returns the entities returned by a query."
  [query db & args]
  (->> (apply q query db args)
       (mapv (fn [item]
               (assert (= 1 (count item)))
               (d/entity db (first item))))))

(defn qfs
  "Returns the first of each query result."
  [query db & args]
  (->> (apply q query db args)
       (mapv first)))
