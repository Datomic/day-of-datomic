;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require '[datomic.api :as d])

;; bind vars
(d/q '[:find [?first ?last]
      :in ?first ?last]
    "John" "Doe")

;; bind tuples
(d/q '[:find [?first ?last]
      :in [?first ?last]]
    ["John" "Doe"])

;; bind a collection
(d/q '[:find [?first ...]
      :in [?first ...]]
    ["John" "Jane" "Phineas"])

;; bind a relation
(d/q '[:find [?first ...]
      :in [[?first ?last]]]
    [["John" "Doe"]
     ["Jane" "Doe"]])

;; bind a "database"
(d/q '[:find [?first ...]
      :where [_ :first-name ?first]]
    [[42 :first-name "John"]
     [42 :last-name "Doe"]
     [43 :first-name "Jane"]
     [43 :last-name "Doe"]])



