(use :reload 'datomic.samples.repl)
(easy!)
(def conn (scratch-conn))

;; in data, use data literals for tempids
(def tx-data [{:db/id #db/id[:db.part/user]
               :db/doc "Example 1"}])
(transact conn tx-data)

;; in code, call tempid to create tempids
(let [id (tempid :db.part/user)
      doc "Example 2"]
  (transact conn [{:db/id id :db/doc doc}]))

;; same argument applies to functions:
;; use #db/fn literals in data
;; use Peer.function or d/function in code

;; broken, uses db/fn literal in code
(transact conn [{:db/id #db/id [:db.part/user]
                 :db/ident :hello
                 :db/fn #db/fn {:lang "clojure"
                                :params []
                                :code '(println :hello)}}])

;; corrected: used d/function to construct function
(transact conn [{:db/id (d/tempid :db.part/user)
                 :db/ident :hello
                 :db/fn (d/function {:lang "clojure"
                                     :params []
                                     :code '(println :hello)})}])
(d/invoke (db conn) :hello)







