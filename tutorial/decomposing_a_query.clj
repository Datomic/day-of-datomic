;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(require '[datomic.api :as d])
(set! *print-length* 100)

;; in memory "database"
(def kvs (into [] (for [e (range 2000)
                        a [:a :b :c :d :e]]
                    [e a e])))
(count kvs)

;; This query is intended to be gibberish.  Knowing nothing about the
;; domain or the data, how would you figure out why this query is slow?
(time (count (d/query {:query '[:find ?e1 ?e2
                                :where
                                [?e1 :a ?v1]
                                [?e2 :a ?v2]
                                [?e1 :a 10]
                                [?e2 :a ?e1]]
                       :args [kvs]
                       :timeout 10000})))
;; ~8 seconds, 1 result

;; Drop clauses from the end one at a time
(time (count (d/query {:query '[:find ?e1 ?e2
                                :where
                                [?e1 :a ?v1]
                                [?e2 :a ?v2]
                                [?e1 :a 10]]
                       :args [kvs]
                       :timeout 10000})))
;; ~7 seconds, 2e3 results

;; Drop another clause
(time (count (d/query {:query '[:find ?e1 ?e2
                                :where
                                [?e1 :a ?v1]
                                [?e2 :a ?v2]]
                       :args [kvs]
                       :timeout 10000})))
;; 13 seconds, 4e6 results


;; You may need to adjust :find to remove variables no longer in the query
(time (count (d/query {:query '[:find ?e1
                                :where
                                [?e1 :a ?v1]]
                       :args [kvs]
                       :timeout 10000})))
;; 0.001 seconds, 2000 results


;; Looking back, the addition of the second clause blew out the number
;; of intermediate results and the query time. what if we change the
;; order and move that clause last?
(time (count (d/query {:query '[:find ?e1 ?e2
                                :where
                                [?e1 :a ?v1]
                                [?e1 :a 10]
                                [?e2 :a ?e1]
                                [?e2 :a ?v2]]
                       :args [kvs]
                       :timeout 10000})))
;; 0.01 seconds, 1 result

;; Of course you can write a much better query by *understanding* its
;; intention (shown below). But it is interesting that you don't *have* to
;; understand the query to find problems by reordering or removing
;; clauses. You could even write a program to do it for you.


;; Anayzing the Query based on domain knowledge

;; 1. Lots of datoms match [?e1 :a ?v1]
;; 2. Lots of daotms match [?e2 :a ?v2]
;; 3. 1 & 2 have no vars in common, so this makes a cross-product. Danger!
;; 4. Very few datoms match [?e1 :a 10]
;; 5. Given 4, 1 is not needed at all
;; 6. Very few datoms match [?e2 :a ?e1]
;; 7. Given 6, 2 is not needed at all
;; 8. Given 4 & 6, ?e1 is known at input time
;; So:
(d/query {:query '[:find ?e1 ?e2
                   :in $ ?e1
                   :where [?e2 :a ?e1]]
          :args [kvs 10]
          :timeout 10000})
