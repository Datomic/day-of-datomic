(import 'java.util.concurrent.LinkedBlockingQueue)
(use 'datomic.samples.repl)

(doc easy!)
(easy!)

(defn pipeline
  [conn txes in-flight]
  (let [q (LinkedBlockingQueue. in-flight)]
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

(doc scratch-conn)

(def conn (scratch-conn))

(def txes (read-all (io/resource "day-of-datomic/social-news.edn")))

(pipeline conn txes 10)
