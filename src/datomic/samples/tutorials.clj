(ns datomic.samples.tutorials
  (:use datomic.samples.io
        datomic.samples.repl)
  (:require
   [clojure.java.io :as io]
   [datomic.api :as d]))

(defn tutorial-seq
  []
  (->> (file-seq (io/file "tutorial"))
       (filter #(.endsWith (.getName ^java.io.File %) ".clj"))))

(defn -main
  "Run all the tutorials"
  [& _]
  (doseq [file (tutorial-seq)]
    (println "\n;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;")
    (println ";; Transcript for " file)
    (transcript (read-all (io/reader file)))
    (d/shutdown true)))
