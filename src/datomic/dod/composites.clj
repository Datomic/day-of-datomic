;; Copyright (c) Cognitect, Inc. All rights reserved.

(ns datomic.dod.composites
  (:require [datomic.api :as d]))

(defn establish-composite
  "Reasserts all values of attr, in batches of batch-size, with
  pacing-sec pause between transactions. This will establish values
  for any composite attributes built from attr."
  [conn {:keys [attr batch-size pacing-sec]}]
  (let [db (d/db conn)
        es (d/datoms db :aevt attr)]
    (doseq [batch (partition-all batch-size es)]
      (let [es (into #{} (map :e batch))
            result @(d/transact conn (map (fn [{:keys [e v]}]
                                            [:db/add e attr v])
                                          batch))
            added (transduce
                    (comp (map :e) (filter es))
                    (completing (fn [x ids] (inc x)))
                    0
                    (:tx-data result))]
        (println {:batch-size batch-size :first-e (:e (first batch)) :added added})
        (Thread/sleep (* 1000 pacing-sec))))))
