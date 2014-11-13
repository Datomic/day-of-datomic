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

(set! *warn-on-reflection* true)

(import 'java.util.concurrent.LinkedBlockingQueue)
(defn pipeline
  [conn txes in-flight]
  (let [q (LinkedBlockingQueue. (int in-flight))]
    {:result-future (future
                      (loop [{:keys [tx fut] :as current} (.take q)]
                        (when-not (= current :done)
                          (if fut
                            (if-let [res (try
                                           (deref fut 10000 nil)
                                           (catch Throwable t nil))]
                              (println "success")
                              (println "failure after submission. insert retry logic here for" tx))
                            (println "failure prior to submission. insert retry logic here for" tx))
                          (recur (.take q))))
                      :done)
     :submit-future (future
                      (doseq [tx txes]
                        (.put q {:tx  tx
                                 :fut (try
                                        (d/transact-async conn tx)
                                        (catch Throwable t nil))}))
                      (.put q :done)
                      :done)}))

(def conn (repl/scratch-conn))

(def txes (repl/read-all (repl/resource "day-of-datomic/social-news.edn")))

(pipeline conn txes 10)
