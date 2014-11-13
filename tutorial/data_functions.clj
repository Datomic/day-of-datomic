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

(def conn (repl/scratch-conn))
(repl/transact-all conn (repl/resource "day-of-datomic/social-news.edn"))
(repl/transact-all conn (repl/resource "day-of-datomic/clojure-data-functions.edn"))

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
(assert (= "Hello, John" (hello "John")))

;; install the function in a database, under the name :hello
@(d/transact
  conn
  [{:db/id (d/tempid :db.part/user)
    :db/doc "Example function returning a greeting"
    :db/ident :hello
    :db/fn hello}])

(def db (d/db conn))

;; retrieve function from db and call it
(def hello-from-db (d/entity db :hello))
(:db/doc hello-from-db)
((:db/fn hello-from-db) "John")

;; get a validation function from the database
(def validate-person
  (-> (d/entity db :validatePerson) :db/fn))

;; validate an invalid person
(-> (validate-person {:user/email "jdoe@example.com"})
    repl/should-throw)

;; validate a valid person
(validate-person {:user/email "jdoe@example.com"
                  :user/firstName "John"
                  :user/lastName "Doe"})

;; get a constructor function from the database
(def construct-person
  (-> (d/entity db :constructPerson) :db/fn))

;; test constructing an invalid person locally 
(-> (construct-person db {})
    repl/should-throw)

(d/q '[:find [?n]
       :where [_ :db/ident ?n]]
     db)
;; test constructing a valid person locally
(construct-person
 db
 {:user/email "jdoe@example.com"
  :user/firstName "John"
  :user/lastName "Doe"})

;; create a person in the database!
@(d/transact
  conn
  [[:constructPerson
    {:user/email "jdoe@example.com"
     :user/firstName "John"
     :user/lastName "Doe"}]])

(def db (d/db conn))

;; get a view helper function from the database
(def display-name
  (-> (d/entity db :displayName) :db/fn))

(display-name (d/entity db [:user/email "jdoe@example.com"]))

(d/release conn)
