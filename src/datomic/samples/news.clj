(ns datomic.samples.news
  (:import java.util.Random)
  (:require
   [clojure.data.generators :as gen]
   [clojure.java.io :as io]
   [datomic.api :as d]
   [datomic.samples.query :refer (qe)]
   [datomic.samples.io :as dio]))

(defn generate-some-comments
  "Generates tranaction data for some comments"
  [db n]
  (let [story-ids (->> (d/q '[:find ?e :where [?e :story/url]] db) (mapv first))
        user-ids (->> (d/q '[:find ?e :where [?e :user/email]] db) (mapv first))
        comment-ids (->> (d/q '[:find ?e :where [?e :comment/author]] db) (mapv first))
        choose1 (fn [n] (when (seq n) (gen/rand-nth n)))]
    (assert (seq story-ids))
    (assert (seq user-ids))
    (->> (fn []
           (let [comment-id (d/tempid :db.part/user)
                 parent-id (or (choose1 comment-ids) (choose1 story-ids))]
             [[:db/add parent-id :comments comment-id]
              [:db/add comment-id :comment/author (choose1 user-ids)]
              [:db/add comment-id :comment/body "blah"]]))
         (repeatedly n)
         (mapcat identity))))

(defn setup-sample-db-1
  [uri]
  (d/create-database uri)
  (let [conn (d/connect uri)]
    (doseq [schema ["day-of-datomic/social-news.edn"
                    "day-of-datomic/provenance.edn"]]
      (->> (io/resource schema)
           (dio/transact-all conn)))
    (let [[[ed]] (seq (d/q '[:find ?e :where [?e :user/email "editor@example.com"]]
                           (d/db conn)))]
      @(d/transact conn [[:db/add ed :user/firstName "Edward"]]))
    (binding [gen/*rnd* (Random. 42)]
      (dotimes [_ 4]
        @(d/transact conn (generate-some-comments (d/db conn) 5)))
      conn)))
