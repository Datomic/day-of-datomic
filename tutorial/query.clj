;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

;; The examples below parallel http://docs.datomic.com/query.html
;; sample data at https://github.com/Datomic/mbrainz-sample

;; get connected
(require '[datomic.api :as d]
         '[datomic.samples.repl :as repl])
(def uri "datomic:dev://localhost:4334/mbrainz-1968-1973")
(def conn (d/connect uri))
(def db (d/db conn))
(set! *print-length* 100)

(d/q '[:find ?release-name
       :where [_ :release/name ?release-name]]
     db)

(d/q '[:find ?release-name
       :in $
       :where [$ _ :release/name ?release-name]]
     db)

(d/q '[:find ?release-name
       :in $ ?artist-name
       :where [?artist :artist/name ?artist-name]
              [?release :release/artists ?artist]
              [?release :release/name ?release-name]]
     db "John Lennon")

(d/q '[:find ?release
       :in $ [?artist-name ?release-name]
       :where [?artist :artist/name ?artist-name]
              [?release :release/artists ?artist]
              [?release :release/name ?release-name]]
     db ["John Lennon" "Mind Games"])

(d/q '[:find ?release-name
       :in $ [?artist-name ...]
       :where [?artist :artist/name ?artist-name]
              [?release :release/artists ?artist]
              [?release :release/name ?release-name]]
     db ["Paul McCartney" "George Harrison"])

(d/q '[:find ?release
       :in $ [[?artist-name ?release-name]]
       :where [?artist :artist/name ?artist-name]
              [?release :release/artists ?artist]
              [?release :release/name ?release-name]]
     db [["John Lennon" "Mind Games"] ["Paul McCartney" "Ram"]])

(d/q '[:find ?artist-name ?release-name
       :where [?release :release/name ?release-name]
              [?release :release/artists ?artist]
              [?artist :artist/name ?artist-name]]
     db)

(d/q '[:find [?release-name ...]
       :in $ ?artist-name
       :where [?artist :artist/name ?artist-name]
              [?release :release/artists ?artist]
              [?release :release/name ?release-name]]
     db "John Lennon")

(d/q '[:find [?year ?month ?day]
       :in $ ?name
       :where [?artist :artist/name ?name]
              [?artist :artist/startDay ?day]
              [?artist :artist/startMonth ?month]
              [?artist :artist/startYear ?year]]
     db "John Lennon")

(d/q '[:find ?year .
       :in $ ?name
       :where [?artist :artist/name ?name]
              [?artist :artist/startYear ?year]]
     db "John Lennon")

(d/q '[:find (count ?eid) .
       :where [?eid :artist/name]
              (not [?eid :artist/country :country/CA])]
     db) 

(d/q '[:find (count ?artist) .
       :where [?artist :artist/name]
              (not-join [?artist]
                [?release :release/artists ?artist]
                [?release :release/year 1970])]
     db)

(d/q '[:find (count ?r) .
       :where [?r :release/name "Live at Carnegie Hall"]
       (not-join [?r]
                 [?r :release/artists ?a]
                 [?a :artist/name "Bill Withers"])]
     db)

(d/q '[:find (count ?artist) .
       :where (or [?artist :artist/type :artist.type/group]
              (and [?artist :artist/type :artist.type/person]
                   [?artist :artist/gender :artist.gender/female]))]
     db)

(d/q '[:find (count ?release) .
       :where [?release :release/name]
       (or-join [?release]
         (and [?release :release/artists ?artist]
              [?artist :artist/country :country/CA])
         [?release :release/year 1970])]
     db)

(d/q '[:find ?name ?year
       :where [?artist :artist/name ?name]
              [?artist :artist/startYear ?year]
              [(< ?year 1600)]]
     db)

(d/q '[:find ?track-name ?minutes
       :in $ ?artist-name
       :where [?artist :artist/name ?artist-name]
              [?track :track/artists ?artist]
              [?track :track/duration ?millis]
              [(quot ?millis 60000) ?minutes]
              [?track :track/name ?track-name]]
     db "John Lennon")

;; expression clauses do not nest
#_(d/q '[:find ?celsius .
       :in ?fahrenheit
       :where [(/ (- ?fahrenheit 32) 1.8) ?celsius]]
     212)

(d/q '[:find ?celsius .
       :in ?fahrenheit
       :where [(- ?fahrenheit 32) ?f-32]
              [(/ ?f-32 1.8) ?celsius]]
     212)

(d/q '[:find ?artist-name ?year
       :in $ [?artist-name ...]
       :where [?artist :artist/name ?artist-name]
              [(get-else $ ?artist :artist/startYear "N/A") ?year]]
     db, ["Crosby, Stills & Nash" "Crosby & Nash"])

(d/q '[:find [?e ?attr ?name]
       :in $ ?e
       :where [(get-some $ ?e :country/name :artist/name) [?attr ?name]]]
     db :country/US)

(d/q '[:find ?entity ?name ?tx ?score
       :in $ ?search
       :where [(fulltext $ :artist/name ?search) [[?entity ?name ?tx ?score]]]]
     db "Jane")

(d/q '[:find ?name
       :where [?artist :artist/name ?name]
              [(missing? $ ?artist :artist/startYear)]]
     db)

(d/q '[:find [?tx ...]
       :in ?log
       :where [(tx-ids ?log 1000 1050) [?tx ...]]]
     (d/log conn))

(d/q '[:find [?e ...]
       :in ?log ?tx
       :where [(tx-data ?log ?tx) [[?e]]]]
     (d/log conn) 13194139534312)

(d/q '[:find ?k ?v
       :where [(System/getProperties) [[?k ?v]]]])

(d/q '[:find ?k ?v
       :where [(System/getProperties) [[?k ?v]]]
              [(.endsWith ?k "version")]])

(d/q '[:find [?prefix ...]
       :in [?word ...]
       :where [(subs ?word 0 5) ?prefix]]
     ["hello" "antidisestablishmentarianism"])

(d/q '[:find [?aname ...]
       :where [?attr 42 _]
              [?attr :db/ident ?aname]]
     db)

;; avoid dynamic attr specs unless you really need them
(d/q '[:find [?aname ...]
       :in $ [?property ...]
       :where [?attr ?property _]
              [?attr :db/ident ?aname]]
     db [:db/unique])

;; the following thee queries are equivalent
(d/q '[:find [?artist-name ...]
       :in $ ?country
       :where [?artist :artist/name ?artist-name]
              [?artist :artist/country ?country]]
     db [:country/name "Belgium"])

(d/q '[:find [?artist-name ...]
       :in $ ?country
       :where [?artist :artist/name ?artist-name]
              [?artist :artist/country ?country]]
     db :country/BE)

(d/q '[:find [?artist-name ...]
       :in $ ?country
       :where [?artist :artist/name ?artist-name]
              [?artist :artist/country ?country]]
     db 17592186045516)

;; this will not work as desired
(d/q '[:find [?artist-name ...]
       :in $ ?country [?reference ...]
       :where [?artist :artist/name ?artist-name]
              [?artist ?reference ?country]]
     db :country/BE [:artist/country])

;; fixes the previous query
(d/q '[:find [?artist-name ...]
       :in $ ?country [?reference ...]
       :where [(datomic.api/entid $ ?country) ?country-id]
              [?artist :artist/name ?artist-name]
              [?artist ?reference ?country-id]]
     db :country/BE [:artist/country])

;; rules examples

;; aggregate examples

;; incorrect query
(d/q '[:find (sum ?heads) .
       :in [[_ ?heads]]]
     [["Cerberus" 3]
      ["Medusa" 1]
      ["Cyclops" 1]
      ["Chimera" 1]])

;; fixes previous query
(d/q '[:find (sum ?heads) .
       :with ?monster
       :in [[?monster ?heads]]]
     [["Cerberus" 3]
      ["Medusa" 1]
      ["Cyclops" 1]
      ["Chimera" 1]])

(d/q '[:find [(min ?dur) (max ?dur)]
        :where [_ :track/duration ?dur]]
     db)

(d/q '[:find [(count ?name) (count-distinct ?name)]
       :with ?artist
       :where [?artist :artist/name ?name]]
     db)

(d/q '[:find (count ?track)
       :where [?track :track/name]]
     db)

(d/q '[:find (sum ?count) .
       :with ?medium
       :where [?medium :medium/trackCount ?count]]
     db)

(d/q '[:find ?year (median ?namelen) (avg ?namelen) (stddev ?namelen)
       :with ?track
       :where [?track :track/name ?name]
              [(count ?name) ?namelen]
              [?medium :medium/tracks ?track]
              [?release :release/media ?medium]
              [?release :release/year ?year]]
     db)

(d/q '[:find (distinct ?v) .
       :in [?v ...]] 
     [1 1 2 2 2 3])

(d/q '[:find [(min 5 ?millis) (max 5 ?millis)]
       :where [?track :track/duration ?millis]]
     db)

(d/q '[:find [(rand 2 ?name) (sample 2 ?name)]
       :where [_ :artist/name ?name]]
     db)

(defn mode
  [vals]
  (->> (frequencies vals)
       (sort-by (comp - second))
       ffirst))

;; This works when evaluating forms one at a time, but throws a
;; NullPointerException under 'lein run -m datomic.samples.tutorial'
#_(d/q '[:find (user/mode ?track-count) .
         :with ?media
         :where [?media :medium/trackCount ?track-count]]
       db)

;; use d/query to specify a timeout
;; should throw an exception
(-> (d/query {:query   '[:find ?track-name
                         :in $ ?artist-name
                         :where [?track :track/artists ?artist]
                         [?track :track/name ?track-name]
                         [?artist :artist/name ?artist-name]]
              :args    [db "John Lennon"]
              :timeout 100})
    repl/should-throw)
