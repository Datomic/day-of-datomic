;; work through at the REPL, evaulating each form

(use :reload 'datomic.samples.repl)

(doc easy!)
(easy!)

(doc scratch-conn)

(def conn (scratch-conn))

(doc transact-all)

(transact-all conn (io/resource "day-of-datomic/social-news.dtm"))

(doc qes)
(source qes)
(defpp all-stories
  "All stories"
  (qes '[:find ?e :where [?e :story/url]] (db conn)))

(doc d/tempid)
(doc tempid)
(defpp new-user-id (tempid))

(defpp upvote-all-stories
  "Transaction data for new-user-id to upvote all stories"
  (mapv
   (fn [[story]] [:db/add new-user-id :user/upVotes (:db/id story)])
   all-stories))

(defpp new-user
  "Transaction data for a new user"
  [{:db/id new-user-id
    :user/email "john@example.com"
    :user/firstName "John"
    :user/lastName "Doe"}])

(defpp upvote-tx-result
  "In a single transaction, create new user and upvote all stories"
  (->> (concat upvote-all-stories new-user)
       (d/transact conn)))

(defpp change-user-name-result
  "Demonstrates upsert. Tempid will resolve to existing id to
   match specified :user/email."
  (d/transact
   conn
   [{:user/email "john@example.com" ;; this finds the existing entity
     :db/id #db/id [:db.part/user]  ;; will be replaced by existing id
     :user/firstName "Johnathan"}]))

(doc qe)
(source qe)

(defpp john
  (qe '[:find ?e :where [?e :user/email "john@example.com"]]
      (db conn)))

(defpp johns-upvote-for-pg
  (qe '[:find ?story
        :in $ ?e
        :where [?e :user/upVotes ?story]
        [?story :story/url "http://www.paulgraham.com/avg.html"]]
      (db conn)
      (:db/id john)))

(defpp john-retracts-upvote-result
  (d/transact
   conn
   [[:db/retract (:db/id john) :user/upVotes (:db/id johns-upvote-for-pg)]]))

(defpp john
  (find-by (db conn) :user/email "john@example.com"))

;; should now be only two, since one was retracted
(get john :user/upVotes)

(defpp data-that-retracts-johns-upvotes
  (->> (q '[:find ?op ?e ?a ?v
            :in $ ?op ?e ?a
            :where [?e ?a ?v]]
          (db conn)
          :db/retract
          (:db/id john)
          :user/upVotes)
       (into [])))

(d/transact conn data-that-retracts-johns-upvotes)

(defpp john
  (find-by (db conn) :user/email "john@example.com"))

;; all gone
(get john :user/upVotes)

(doc gen-users-with-upvotes)

(defpp ten-new-users
  (gen-users-with-upvotes (mapv first all-stories) "user" 10))

(def add-ten-new-users-result
  (d/transact conn ten-new-users))

;; how many users are there? 
(count (q '[:find ?e ?v :where [?e :user/email ?v]] (db conn)))

;; how many users have upvoted something?
(count (q '[:find ?e
            :where [?e :user/email]
                   [?e :user/upVotes]]
          (db conn)))

;; Datomic does not need a left join to keep entities missing
;; some attribute. Just leave that attribute out of the query,
;; and then ask for it on the entity.
(defpp users-with-emails-and-upvotes
  (->> (find-all-by (db conn) :user/email)
       (mapv
        (fn [[ent]]                                
          {:email (:user/email ent)
           :upvoted (mapv :story/url (:user/upVotes ent))}))))


;; if (contra the previous example) you want to do the left
;; join work in the query itself, you can use something like
;; the "maybe" function below. This is likely to get first
;; class support in Datomic in the future.
(defn maybe
  "Returns the set of attr for e, or nil if e does not possess
   any values for attr."
  [db e attr]
  (seq (map :a (d/datoms db :eavt e attr))))

;; find all users 
(q '[:find ?e ?upvote
     :where
     [?e :user/email]
     [(user/maybe $ ?e :user/upVotes) ?upvote]]
   (db conn))
