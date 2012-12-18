;; work through at the REPL, evaulating each form
(use 'datomic.samples.repl)
(easy!)

(def conn (scratch-conn))
(transact-all conn (io/resource "day-of-datomic/social-news.edn"))

(def atempid (d/tempid :db.part/user))

;; create a story and some comments
(let [[story comment-1 comment-2] (repeatedly #(d/tempid :db.part/user))
        {:keys [tempids db-after]}
        @(d/transact conn [{:db/id story
                            :story/title "Getting Started"
                            :story/url "http://docs.datomic.com/getting-started.html"}
                           {:db/id comment-1
                            :comment/body "It woud be great to learn about component attributes."
                            :_comments story}
                           {:db/id comment-2
                            :comment/body "I agree."
                            :_comments comment-1}])]
  (def story (d/resolve-tempid db-after tempids story))
  (def comment-1 (d/resolve-tempid db-after tempids comment-1))
  (def comment-2 (d/resolve-tempid db-after tempids comment-2)))

;; component attributes are automatically loaded by touch
(-> conn d/db (d/entity story) d/touch)

;; what does db.fn/retractEntity do?
(-> conn d/db (d/entity :db.fn/retractEntity) :db/doc)

;; retract the story
(def retracted-es (->> (d/transact conn [[:db.fn/retractEntity story]])
                       deref
                       :tx-data
                       (remove #(:added %))
                       (map :e)
                       (into #{})))

;; retraction recursively retracts component comments
(assert (= retracted-es #{story comment-1 comment-2}))




