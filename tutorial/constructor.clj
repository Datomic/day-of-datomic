;; work through at the REPL, evaulating each form
(use 'datomic.samples.repl)
(easy!)

(def conn (scratch-conn))
(def schema [{:db/id #db/id [:db.part/db],
              :db/ident :user/name,
              :db/valueType :db.type/string,
              :db/cardinality :db.cardinality/one,
              :db/index true,
              :db.install/_attribute :db.part/db}
             {:db/id #db/id [:db.part/db],
              :db/ident :user/email,
              :db/valueType :db.type/string,
              :db/cardinality :db.cardinality/one,
              :db/index true,
              :db.install/_attribute :db.part/db}
             {:db/id #db/id [:db.part/db],
              :db/ident :user/token,
              :db/valueType :db.type/string,
              :db/cardinality :db.cardinality/one,
              :db/index true,
              :db.install/_attribute :db.part/db}])
(d/transact conn schema)

(def construct-user-map
  "Returns map that could be added to transaction data to create
   a new user, or nil if user exists"
  #db/fn {:lang :clojure
          :params [db id email name]
          :code (when-not (seq (d/q '[:find ?e
                                      :in $ ?email
                                      :where [?e :user/email ?email]]
                                    db email))
                  {:db/id id
                   :user/email email
                   :user/name name})})

;; get a database *value* for testing construct-user-map
;; all tests can be of pure functions!
(def dbval (d/db conn))

;; create a map for jdoe
(def construct-jdoe (construct-user-map dbval (d/tempid :db.part/user) "jdoe@example.com" "John Doe"))

;; another database *value*
(def db-with-jdoe (:db-after (d/with dbval [construct-jdoe])))

;; jdoe already exists, should return nil
(construct-user-map db-with-jdoe
                    (d/tempid :db.part/user)
                    "jdoe@example.com"
                    "Jonathan Doe")

(def user-errors
  "Returns a map of attribute-name => error-msgs with
   for display in a user interface "
  #db/fn {:lang :clojure
          :params [db user]
          :code (let [name-count (count (:user/name user))]
                  (when-not (<= 3 name-count 15)
                    {:user/name ["Username must be between 3 and 15 characters"]}))})

;; jdoe is valid, should return nil
(user-errors dbval construct-jdoe)

;; validation failure
(user-errors dbval (construct-user-map dbval (d/tempid :db.part/user) "jdoe@example.com"
                                    "John WhoHasAnOverlyLongName"))

;; install the construct and validate functions in the database
(d/transact conn [{:db/id (d/tempid :db.part/user)
                   :db/ident :user/construct-map
                   :db/fn construct-user-map}
                  {:db/id (d/tempid :db.part/user)
                   :db/ident :user/errors
                   :db/fn user-errors}])

;; another database value
(def db-with-fns (d/db conn))

;; double check invoking from database
(d/invoke db-with-fns :user/construct-map db-with-fns (d/tempid :db.part/user) "jdoe@example.com" "John Doe")
(d/invoke db-with-fns :user/errors db-with-fns construct-jdoe)

(def create-user
  "Returns transaction data to create a new user, throwing
   an exception if validations fail."
  #db/fn {:lang :clojure
          :params [db id email name]
          :code (if-let [user (d/invoke db :user/construct-map db id email name)]
                  (if-let [errors (d/invoke db :user/errors db user)]
                    (throw (ex-info "Validation failed" errors))
                    [user])
                  (throw (ex-info "Validation failed"
                                  {:user/email ["Already exists"]})))})

;; create with validation
(create-user db-with-fns (d/tempid :db.part/user) "jdoe@example.com" "John Doe")

;; should fail with exception
(-> (create-user db-with-fns (d/tempid :db.part/user) "jdoe@example.com" "John WithOverlyLongLastName")
    should-throw)

;; install the create function in the database
(d/transact conn [{:db/id (d/tempid :db.part/user)
                 :db/ident :user/create
                 :db/fn create-user}])


;; create John Doe. Tada!
(d/transact conn [[:user/create (d/tempid :db.part/user) "jdoe@example.com" "John Doe"]])

;; Where are we?
;; Note that all three functions (user/construct-map, user/errors, and
;; user/create) can be run either from the peer or inside a
;; transaction. In a web application, you would likely call
;; user/construct-map and user/errors to fail early on known-invalid
;; data, without ever touching the transactor. You could then call
;; user/create to do the work in the transactor, ensuring that the
;; database respected your domain rules. You could also run
;; user/errors against all the users in the database, to see if any of
;; them got into an invalid state somehow (e.g. if validation rules
;; expanded over time, older users might not conform to them.)


;; Enhancement request: Import data from a legacy system, keeping the
;; legacy id for reference. Only showing users here, but note that the
;; legacy system manages other entities we will be importing as well,
;; so the id is :legacy/uuid, not :user/legacy-uuid.

(def legacy-schema [{:db/id #db/id [:db.part/db],
                     :db/ident :legacy/uuid,
                     :db/valueType :db.type/uuid,
                     :db/cardinality :db.cardinality/one,
                     :db/index true,
                     :db.install/_attribute :db.part/db}])
(d/transact conn legacy-schema)

;; because we designed our data functions to be explicit about
;; databases and ids, we can compose new information about users
;; without touching the code for user/create:

(def jane-uuid #uuid "5083ec09-61b3-4d6d-a945-78c4db0cec06")
(let [id (d/tempid :db.part/user)]
  (d/transact conn [[:user/create id "janedoe@example.com" "Jane Doe"]
                  [:db/add id :legacy/uuid jane-uuid]
                  [:db/add id :db/doc "Jane was in legacy system..."]]))


;; Enhancement request: Batch user creation into a larger transaction
;; Effort required: none
(d/transact conn [[:user/create (d/tempid :db.part/user) "jack@example.com" "Jack Doe"]
                  [:user/create (d/tempid :db.part/user) "jill@example.com" "Jill Doe"]])


;; Enhancement request: Record user creation requests now, play them
;; back later.
;; Effort required: none
(def recorded-tx [[:user/create (d/tempid :db.part/user) "jebd@example.com" "Jebediah Doe"]])
(d/transact conn recorded-tx)

;; Enhancement request: Decouple user creation from the user interface
;; using a queue.
;; Effort required: Put recorded-txes on a queue.  Might choose to
;; also give transactions client-generated identity, which could also
;; be layered in compositionally.
