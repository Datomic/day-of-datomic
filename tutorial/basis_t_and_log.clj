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

;; sample data at https://github.com/Datomic/mbrainz-sample
(def uri "datomic:dev://localhost:4334/mbrainz-1968-1973")
(def conn (d/connect uri))
(def db (d/db conn))

;; basis-t is t of most recent transaction 
(def basis-t (d/basis-t db))
(def basis-tx (d/t->tx basis-t))

;; facts about the most recent transaction
(d/pull db '[*] basis-tx)

;; log is an index of transactions
(def log (d/log conn))

;; how many datoms in most recent transaction?
(-> (d/tx-range log basis-tx (inc basis-tx))
    seq first :data count)

