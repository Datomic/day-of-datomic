(use 'datomic.samples.repl)
(easy!)

(def conn (scratch-conn))

(def schema-map (read-string (slurp (io/resource "day-of-datomic/schema.edn"))))

(has-attribute? (d/db conn) :story/title)

;; install the :day-of-datomic/provenance schema
(ensure-schemas conn :day-of-datomic/schema schema-map :day-of-datomic/provenance)

;; now we have the provenance attributes
(has-attribute? (d/db conn) :source/user)

;; and we have the social-news attributes, because provenance
;; depended on them
(has-attribute? (d/db conn) :story/title)

