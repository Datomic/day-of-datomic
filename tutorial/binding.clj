(use 'datomic.samples.repl)
(easy!)

;; bind vars
(q '[:find ?first ?last
     :in ?first ?last]
   "John" "Doe")

;; bind tuples
(q '[:find ?first ?last
     :in [?first ?last]]
   ["John" "Doe"])

;; bind a collection
(q '[:find ?first
     :in [?first ...]]
   ["John" "Jane" "Phineas"])

;; bind a relation
(q '[:find ?first
     :in [[?first ?last]]]
   [["John" "Doe"]
    ["Jane" "Doe"]])

;; bind a "database"
(q '[:find ?first
     :where [_ :first-name ?first]]
   [[42 :first-name "John"]
    [42 :last-name "Doe"]
    [43 :first-name "Jane"]
    [43 :last-name "Doe"]])


