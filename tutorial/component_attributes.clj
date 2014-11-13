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
  (def comment-2 (d/resolve-tempid db-after tempids comment-2))
  (def db db-after))

;; component attributes are automatically loaded by touch
(-> db (d/entity story) d/touch)

;; what does db.fn/retractEntity do?
(-> db (d/entity :db.fn/retractEntity) :db/doc)

;; retract the story
(def retracted-es (->> (d/transact conn [[:db.fn/retractEntity story]])
                       deref
                       :tx-data
                       (remove :added)
                       (map :e)
                       (into #{})))

;; retraction recursively retracts component comments
(assert (= retracted-es #{story comment-1 comment-2}))

(d/release conn)



