;   Copyright (c) Metadata Partners, LLC. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns datomic.samples.incanter
  (:require [incanter.core :as incanter]))

;; if incanter exposed dataset protocol, copy could be avoided
(defn entities->dataset
  "Copy a collection of Datomic entities into an incanter dataset"
  ([ents] (entities->dataset (keys (first ents)) ents))
  ([ks ents]
     (when-let [es (seq ents)]
       (incanter/dataset ks (map (fn [ent] (map #(get ent %) ks)) ents)))))

