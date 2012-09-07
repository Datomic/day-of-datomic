;; This file contains code examples for getting-started.html. They are
;; written in clojure, for use with Datomic's interactive repl. You can
;; start the repl by running 'bin/repl' from the datomic directory.
;; Once the repl is running, you can copy code into it or, if invoke it
;; directory from your editor, based on your configuration.

(use '[datomic.api :only [q db] :as d])
(use 'clojure.pprint)

;; store database uri
(def uri "datomic:mem://seattle")

;; create database
(d/create-database uri)

;; connect to database
(def conn (d/connect uri))

;; parse schema dtm file
(def schema-tx (read-string (slurp "samples/seattle/seattle-schema.dtm")))

;; display first statement
(first schema-tx)

;; submit schema transaction
@(d/transact conn schema-tx)

;; parse seed data dtm file
(def data-tx (read-string (slurp "samples/seattle/seattle-data0.dtm")))

;; display first three statements in seed data transaction
(first data-tx)
(second data-tx)
(nth data-tx 2)

;; submit seed data transaction
@(d/transact conn data-tx)

;; find all communities, return entity ids
(def results (q '[:find ?c :where [?c :community/name]] (db conn)))
(count results)

;; get first entity id in results and make an entity map
(def id (ffirst results))
(def entity (-> conn db (d/entity id)))

;; display the entity map's keys
(keys entity)

;; display the value of the entity's community name
(:community/name entity)

;; for each community, display it's name
(let [db (db conn)]
  (pprint (map #(:community/name (d/entity db (first %))) results)))

;; for each community, get its neighborhood and display
;; both names
(let [db (db conn)]
  (pprint (map #(let [entity (d/entity db (first %))]
                  [(:community/name entity)
                   (-> entity :community/neighborhood :neighborhood/name)])
               results)))

;; for each community, get it's neighborhood, then for
;; that neighborhood, get all it's communities, and
;; print out there names
(def community (d/entity (db conn) (ffirst results)))
(def neighborhood (:community/neighborhood community))
(def communities (:community/_neighborhood neighborhood))
(pprint (map :community/name communities))

;; find all communities and their names
(def results (q '[:find ?c ?n :where [?c :community/name ?n]] (db conn)))
(pprint (map second results))

;; find all community names and urls
(pprint (seq (q '[:find ?n ?u
                      :where
                      [?c :community/name ?n]
                      [?c :community/url ?u]]
                    (db conn))))

;; find all categories for community named "belltown"
(pprint (seq (q '[:find ?e ?c
                      :where
                      [?e :community/name "belltown"]
                      [?e :community/category ?c]]
                    (db conn))))

;; find names of all communities that are twitter feeds
(pprint (seq (q '[:find ?n
                      :where
                      [?c :community/name ?n]
                      [?c :community/type :community.type/twitter]]
                    (db conn))))

;; find names of all communities that are in the NE region
(pprint (seq (q '[:find ?c_name
                      :where
                      [?c :community/name ?c_name]
                      [?c :community/neighborhood ?n]
                      [?n :neighborhood/district ?d]
                      [?d :district/region :region/ne]]
                    (db conn))))

;; find names and regions of all communities
(pprint (seq (q '[:find ?c_name ?r_name
                      :where
                      [?c :community/name ?c_name]
                      [?c :community/neighborhood ?n]
                      [?n :neighborhood/district ?d]
                      [?d :district/region ?r]
                      [?r :db/ident ?r_name]]
                    (db conn))))

;; find all communities that are twitter feeds and facebook pages
;; using the same query and passing in type as a parameter
(def query-by-type '[:find ?n
                     :in $ ?t
                     :where
                     [?c :community/name ?n]
                     [?c :community/type ?t]])

(pprint (seq (q query-by-type (db conn) :community.type/twitter)))

(pprint (seq (q query-by-type (db conn) :community.type/facebook-page)))

;; find all communities that are twitter feeds or facebook pages using
;; one query and a list of individual parameters
(pprint (seq (q '[:find ?n ?t
                      :in $ [?t ...]
                      :where
                      [?c :community/name ?n]
                      [?c :community/type ?t]]
                    (db conn)
                    [:community.type/facebook-page :community.type/twitter])))

;; find all communities that are non-commercial email-lists or commercial
;; web-sites using a list of tuple parameters
(pprint (seq (q '[:find ?n ?t ?ot
                      :in $ [?t ?ot]
                      :where
                      [?c :community/name ?n]
                      [?c :community/type ?t]
                      [?c :community/orgtype ?ot]]
                    (db conn)
                    [:community.type/email-list :community.orgtype/community])))

;; find all communities that are non-commercial email-lists or commercial
;; web-sites using a list of tuple parameters
(pprint (seq (q '[:find ?n ?t ?ot
                      :in $ [[?t ?ot]]
                      :where
                      [?c :community/name ?n]
                      [?c :community/type ?t]
                      [?c :community/orgtype ?ot]]
                    (db conn)
                    [[:community.type/email-list :community.orgtype/community]
                     [:community.type/website :community.orgtype/commercial]])))

;; find all community names coming after "C" in alphabetical order
(pprint (seq (q '[:find ?n
                      :where
                      [?c :community/name ?n]
                      [(.compareTo ?n "C") ?res]
                      [(< ?res 0)]]
                    (db conn))))

;; find all communities whose names include the string "Wallingford"
(pprint (seq (q '[:find ?n
                      :where
                      [(fulltext $ :community/name "Wallingford") [[?e ?n]]]]
                    (db conn))))


;; find all communities that are websites and that are about
;; food, passing in type and search string as parameters
(pprint (seq (q '[:find ?name ?cat
                      :in $ ?type ?search
                      :where
                      [?c :community/name ?name]
                      [?c :community/type ?type]
                      [(fulltext $ :community/category ?search) [[?c ?cat]]]]
                    (db conn)
                    :community.type/website
                    "food")))

;; find all names of all communities that are twitter feeds, using rules
(let [rules '[[[twitter ?c]
               [?c :community/type :community.type/twitter]]]]
  (pprint (seq (q '[:find ?n
                        :in $ %
                        :where
                        [?c :community/name ?n]
                        (twitter ?c)]
                      (db conn)
                      rules))))

;; find names of all communities in NE and SW regions, using rules
;; to avoid repeating logic
(let [rules '[[[region ?c ?r]
               [?c :community/neighborhood ?n]
               [?n :neighborhood/district ?d]
               [?d :district/region ?re]
               [?re :db/ident ?r]]]]
  (pprint (seq (q '[:find ?n
                    :in $ %
                    :where
                    [?c :community/name ?n]
                    [region ?c :region/ne]]
                  (db conn)
                  rules)))
  (pprint (seq (q '[:find ?n
                    :in $ %
                    :where
                    [?c :community/name ?n]
                    [region ?c :region/sw]]
                  (db conn)
                  rules))))

;; find names of all communities that are in any of the northern
;; regions and are social-media, using rules for OR logic
(let [rules '[[[region ?c ?r]
               [?c :community/neighborhood ?n]
               [?n :neighborhood/district ?d]
               [?d :district/region ?re]
               [?re :db/ident ?r]]
              [[social-media ?c]
               [?c :community/type :community.type/twitter]]
              [[social-media ?c]
               [?c :community/type :community.type/facebook-page]]
              [[northern ?c]
               (region ?c :region/ne)]
              [[northern ?c]
               (region ?c :region/n)]
              [[northern ?c]
               (region ?c :region/nw)]
              [[southern ?c]
               (region ?c :region/sw)]
              [[southern ?c]
               (region ?c :region/s)]
              [[southern ?c]
               (region ?c :region/se)]]]
  (pprint (seq (q '[:find ?n
                    :in $ %
                    :where
                    [?c :community/name ?n]
                    (northern ?c)
                    (social-media ?c)]
                  (db conn)
                  rules))))

;; Find all transaction times, sort them in reverse order
(def tx-instants (reverse (sort (q '[:find ?when :where [_ :db/txInstant ?when]]
                                       (db conn)))))

;; pull out two most recent transactions, most recent loaded
;; seed data, second most recent loaded schema
(def data-tx-date (ffirst tx-instants))
(def schema-tx-date (first (second tx-instants)))

;; make query to find all communities
(def communities-query '[:find ?c :where [?c :community/name]])

;; find all communities as of schema transaction
(let [db-asof-schema (-> conn db (d/as-of schema-tx-date))]
  (println (count (seq (q communities-query db-asof-schema)))))

;; find all communities as of seed data transaction
(let [db-asof-data (-> conn db (d/as-of data-tx-date))]
  (println (count (seq (q communities-query db-asof-data)))))

;; find all communities since seed data transaction
(let [db-since-data (-> conn db (d/since data-tx-date))]
  (println (count (seq (q communities-query db-since-data)))))


;; parse additional seed data file
(def new-data-tx (read-string (slurp "samples/seattle/seattle-data1.dtm")))

;; find all communities if new data is loaded
(let [db-if-new-data (-> conn db (d/with new-data-tx))]
  (println (count (seq (q communities-query db-if-new-data)))))

;; find all communities currently in database
(println (count (seq (q communities-query (db conn)))))

;; submit new data transaction
@(d/transact conn new-data-tx)

;; find all communities currently in database
(println (count (seq (q communities-query (db conn)))))

;; find all communities since original seed data load transaction
(let [db-since-data (-> conn db (d/since data-tx-date))]
  (println (count (seq (q communities-query db-since-data)))))


;; make a new partition
@(d/transact conn [{:db/id (d/tempid :db.part/db)
                      :db/ident :events
                      :db.install/_partition :db.part/db}])

;; make a new community
@(d/transact conn [{:db/id (d/tempid :db.part/user)
                      :community/name "Easton"}])

;; update data for a community
(def belltown-id (ffirst (q '[:find ?id
                                  :where
                                  [?id :community/name "belltown"]]
                                (db conn))))

@(d/transact conn [{:db/id belltown-id
                      :community/category "free stuff"}])

;; retract data for a community
@(d/transact conn [[:db/retract belltown-id :community/category "free stuff"]])

;; retract a community entity
(def easton-id (ffirst (q '[:find ?id
                                :where
                                [?id :community/name "Easton"]]
                              (db conn))))

@(d/transact conn [[:db.fn/retractEntity easton-id]])

;; get transaction report queue, add new community again
(def queue (d/tx-report-queue conn))

@(d/transact conn [{:db/id (d/tempid :db.part/user)
                      :community/name "Easton"}])

(when-let [report (.poll queue)]
  (pprint (seq (q '[:find ?e ?aname ?v ?added
                    :in $ [[?e ?a ?v _ ?added]]
                    :where
                    [?e ?a ?v _ ?added]
                    [?a :db/ident ?aname]]
                  (:db-after report)
                  (:tx-data report)))))


