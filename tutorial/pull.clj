;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

;; The examples below parallel http://docs.datomic.com/pull.html
;; sample data at https://github.com/Datomic/mbrainz-sample

;; get connected
(require '[datomic.api :as d])
(def uri "datomic:dev://localhost:4334/mbrainz-1968-1973")
(def conn (d/connect uri))
(def db (d/db conn))
(set! *print-length* 100)

;; entities used in pull examples
(def led-zeppelin [:artist/gid #uuid "678d88b2-87b0-403b-b63d-5da7465aecc3"])
(def mccartney [:artist/gid #uuid "ba550d0e-adac-4864-b88b-407cab5e76af"])
(def dark-side-of-the-moon [:release/gid #uuid "24824319-9bb8-3d1e-a2c5-b8b864dafd1b"])
(def dylan-harrison-sessions [:release/gid #uuid "67bbc160-ac45-4caf-baae-a7e9f5180429"])
(def dylan-harrison-cd (d/q '[:find ?medium .
                              :in $ ?release
                              :where
                              [?release :release/media ?medium]]
                            db
                            (java.util.ArrayList. dylan-harrison-sessions)))
(def ghost-riders (d/q '[:find ?track .
                         :in $ ?release ?trackno
                         :where
                         [?release :release/media ?medium]
                         [?medium :medium/tracks ?track]
                         [?track :track/position ?trackno]]
                       db
                       dylan-harrison-sessions
                       11))
(def concert-for-bangla-desh [:release/gid #uuid "f3bdff34-9a85-4adc-a014-922eef9cdaa5"])

;; attribute name
(d/pull db [:artist/name :artist/startYear] led-zeppelin)
(d/pull db [:artist/country] led-zeppelin)

;; reverse lookup
(d/pull db [:artist/_country] :country/GB)

;; component defaults
(d/pull db [:release/media] dark-side-of-the-moon)

;; noncomponent defaults (same example as "reverse lookup")
(d/pull db [:artist/_country] :country/GB)

;; reverse component lookup
(d/pull db [:release/_media] dylan-harrison-cd)

;; map specifications
(d/pull db [:track/name {:track/artists [:db/id :artist/name]}] ghost-riders)


;; nested map specifications
(d/pull db
        [{:release/media
          [{:medium/tracks
            [:track/name {:track/artists [:artist/name]}]}]}]
        concert-for-bangla-desh)

;; wildcard specification
(d/pull db '[*] concert-for-bangla-desh)

;; wildcard + map specification
(d/pull db '[* {:track/artists [:artist/name]}] ghost-riders)

;; default option
(d/pull db '[:artist/name (:artist/endYear :default 0)] mccartney)

;; default option with different type
(d/pull db '[:artist/name (:artist/endYear :default "N/A")] mccartney)

;; absent attributes are omitted from results
(d/pull db '[:artist/name :died-in-1966?] mccartney)

;; explicit limit
(d/pull db '[(:track/_artists :limit 10)] led-zeppelin)

;; limit + subspec
(d/pull db '[{(:track/_artists :limit 10) [:track/name]}]
        led-zeppelin)

;; limit + subspec + :as option
(d/pull db '[{(:track/_artists :limit 10 :as "Tracks") [:track/name]}] 
        led-zeppelin)

;; no limit
(d/pull db '[(:track/_artists :limit nil)] led-zeppelin)

;; empty results
(d/pull db '[:penguins] led-zeppelin)

;; empty results in a collection
(d/pull db '[{:track/artists [:penguins]}] ghost-riders)


;; Examples below follow http://docs.datomic.com/query.html#pull

;; pull expression in query
(d/q '[:find [(pull ?e [:release/name]) ...]
       :in $ ?artist
       :where [?e :release/artists ?artist]]
     db
     led-zeppelin)

;; dynamic pattern input
(d/q '[:find [(pull ?e pattern) ...]
       :in $ ?artist pattern
       :where [?e :release/artists ?artist]]
     db
     led-zeppelin
     [:release/name])

