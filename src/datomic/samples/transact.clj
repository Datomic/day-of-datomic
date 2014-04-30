;   Copyright (c) Metadata Partners, LLC. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns datomic.samples.transact
  (:require [datomic.api :as d]
            [datomic.samples.query :as q]))

(defn install
  "Install txdata and return the single new entity possessing attr"
  [conn txdata attr]
  (let [t  (d/basis-t (:db-after @(d/transact conn txdata)))
        db (d/db conn)]
    (q/qe '[:find ?e
            :in $ ?attr ?t
            :where [?e ?attr _ ?t]]
          db (d/entid db attr) (d/t->tx t))))
