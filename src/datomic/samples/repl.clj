;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns datomic.samples.repl
  (:import datomic.Util java.util.Random)
  (:require
   [clojure.data.generators :as gen]
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [datomic.api :as d]))

(def db-uri-base "datomic:mem://")

(def resource io/resource)

(defn scratch-conn
  "Create a connection to an anonymous, in-memory database."
  []
  (let [uri (str db-uri-base (d/squuid))]
    (d/delete-database uri)
    (d/create-database uri)
    (d/connect uri)))

(defn read-all
  "Read all forms in f, where f is any resource that can
   be opened by io/reader"
  [f]
  (Util/readAll (io/reader f)))

(defn transact-all
  "Load and run all transactions from f, where f is any
   resource that can be opened by io/reader."
  [conn f]
  (loop [n 0
         [tx & more] (read-all f)]
    (if tx
      (recur (+ n (count (:tx-data  @(d/transact conn tx))))
             more)
      {:datoms n})))

(defn transcript
  "Run all forms, printing a transcript as if forms were
   individually entered interactively at the REPL."
  [forms]
  (binding [*ns* *ns*]
    (let [temp (gensym)]
      (println ";; Executing forms in temp namespace: " temp)
      (in-ns temp)
      (clojure.core/use 'clojure.core 'clojure.repl 'clojure.pprint)
      (doseq [f forms]
        (pp/pprint f)
        (print "=> ")
        (pp/pprint (eval f))
        (println))
      (remove-ns temp)
      :done)))

(defmacro should-throw
  "Runs forms, expecting an exception. Prints descriptive message if
   an exception occurred. Throws if an exception did *not* occur."
  [& forms]
  `(try
    ~@forms
    (throw (ex-info "Expected exception" {:forms '~forms}))
    (catch Throwable t#
      (println "Got expected exception:\n\t" (.getMessage t#)))))

(defn modes
  "Returns the set of modes for a collection."
  [coll]
  (->> (frequencies coll)
       (reduce
        (fn [[modes ct] [k v]]
          (cond
           (< v ct)  [modes ct]
           (= v ct)  [(conj modes k) ct]
           (> v ct) [#{k} v]))
        [#{} 2])
       first))

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
  [conn]
  (doseq [schema ["day-of-datomic/social-news.edn"
                  "day-of-datomic/provenance.edn"]]
    (->> (io/resource schema)
         (transact-all conn)))
  (let [[[ed]] (seq (d/q '[:find ?e :where [?e :user/email "editor@example.com"]]
                         (d/db conn)))]
    @(d/transact conn [[:db/add ed :user/firstName "Edward"]]))
  (binding [gen/*rnd* (Random. 42)]
    (dotimes [_ 4]
      @(d/transact conn (generate-some-comments (d/db conn) 5)))
    conn))

(defn choose-some
  "Pick zero or more items at random from a collection"
  [coll]
  (take (gen/uniform 0 (count coll))
        (gen/shuffle coll)))

(defn gen-users-with-upvotes
  "Make transaction data for example users, possibly with upvotes"
  [stories email-prefix n]
  (mapcat
   (fn [n]
     (let [user-id (d/tempid :db.part/user)
           upvotes (map (fn [story] [:db/add user-id :user/upVotes story])
                        (choose-some stories))]
       (conj
        upvotes
        {:db/id user-id
         :user/email (str email-prefix "-" n "@example.com")})))
   (range n)))

(defn trunc
  "Return a string rep of x, shortened to n chars or less"
  [x n]
  (let [s (str x)]
    (if (<= (count s) n)
      s
      (str (subs s 0 (- n 3)) "..."))))

(def tx-part-e-a-added
  "Sort datoms by tx, then e, then a, then added"
  (reify
   java.util.Comparator
   (compare
    [_ x y]
    (cond
     (< (:tx x) (:tx y)) -1
     (> (:tx x) (:tx y)) 1
     (< (:e x) (:e y)) -1
     (> (:e x) (:e y)) 1
     (< (:a x) (:a y)) -1
     (> (:a x) (:a y)) 1
     (false? (:added x)) -1
     :default 1))))

(defn datom-table
  "Print a collection of datoms in an org-mode compatible table."
  [db datoms]
  (->> datoms
       (map
        (fn [{:keys [e a v tx added]}]
          {"part" (d/part e)
           "e" (format "0x%016x" e)
           "a" (d/ident db a)
           "v" (trunc v 24)
           "tx" (format "0x%x" tx)
           "added" added}))
       (pp/print-table ["part" "e" "a" "v" "tx" "added"])))

