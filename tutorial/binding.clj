(require '[datomic.api :as d])

;; bind vars
(d/q '[:find ?first ?last
      :in ?first ?last]
    "John" "Doe")

;; bind tuples
(d/q '[:find ?first ?last
      :in [?first ?last]]
    ["John" "Doe"])

;; bind a collection
(d/q '[:find ?first
      :in [?first ...]]
    ["John" "Jane" "Phineas"])

;; bind a relation
(d/q '[:find ?first
      :in [[?first ?last]]]
    [["John" "Doe"]
     ["Jane" "Doe"]])

;; bind a "database"
(d/q '[:find ?first
      :where [_ :first-name ?first]]
    [[42 :first-name "John"]
     [42 :last-name "Doe"]
     [43 :first-name "Jane"]
     [43 :last-name "Doe"]])


