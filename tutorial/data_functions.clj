;; work through at the REPL, evaulating each form
(use 'datomic.samples.repl)
(easy!)

(def conn (scratch-conn))
(transact-all conn (io/resource "day-of-datomic/social-news.dtm"))
(transact-all conn (io/resource "day-of-datomic/clojure-data-functions.dtm"))

(dir datomic.api)

(doc d/function)

;; create a function programmatically
(def hello
  (d/function '{:lang :clojure
                :params [name]
                :code (str "Hello, " name)}))

(hello "John")

;; create a function with a literal 
(def hello
  #db/fn {:lang :clojure
          :params [name]
          :code (str "Hello, " name)})

;; test your function outside the database
;; (this is where you would use the test framework of your choice)
(assert (= "Hello, John") (hello "John"))

;; install the function in a database, under the name :hello
(d/transact
 conn
 [{:db/id (d/tempid :db.part/user)
   :db/doc "Example function returning a greeting"
   :db/ident :hello
   :db/fn hello}])

(def dbval (d/db conn))

;; retrieve function from db and call it
(def hello-from-db (d/entity dbval :hello))
(:db/doc hello-from-db)
((:db/fn hello-from-db) "John")

;; get a validation function from the database
(def validate-person
  (-> (d/entity dbval :validatePerson) :db/fn))

;; validate an invalid person
(-> (validate-person {:user/email "jdoe@example.com"})
    should-throw)

;; validate a valid person
(validate-person {:user/email "jdoe@example.com"
                  :user/firstName "John"
                  :user/lastName "Doe"})

;; get a constructor function from the database
(def construct-person
  (-> (d/entity dbval :constructPerson) :db/fn))

;; test constructing an invalid person locally 
(-> (construct-person dbval {})
    should-throw)

;; test constructing a valid person locally
(construct-person
 dbval
 {:user/email "jdoe@example.com"
  :user/firstName "John"
  :user/lastName "Doe"})

;; create a person in the database!
(defpp construct-person-result
  (d/transact
   conn
   [[:constructPerson
     {:user/email "jdoe@example.com"
      :user/firstName "John"
      :user/lastName "Doe"}]]))

;; get a person from the database
(def john (find-by (d/db conn) :user/email "jdoe@example.com"))

;; get a view helper function from the database
(def display-name
  (-> (d/entity dbval :displayName) :db/fn))

(display-name john)
