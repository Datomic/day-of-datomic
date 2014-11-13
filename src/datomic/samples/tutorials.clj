(ns datomic.samples.tutorials
  (:require
   [clojure.java.io :as io]
   [datomic.api :as d]
   [datomic.samples.repl :as repl]))

(defn tutorial-seq
  []
  (->> (file-seq (io/file "tutorial"))
       (filter #(.endsWith (.getName ^java.io.File %) ".clj"))))

(defn -main
  "Run all the tutorials"
  [& _]
  (try
   (doseq [file (tutorial-seq)]
     (println "\n;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;")
     (println ";; Transcript for " file)
     (repl/transcript (repl/read-all (io/reader file))))
   (finally
    (d/shutdown true))))
