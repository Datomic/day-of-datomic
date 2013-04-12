;   Copyright (c) Metadata Partners, LLC. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns datomic.samples.repl
  (:require [clojure.pprint :as pprint]
            [datomic.api :as d]))

(def db-uri-base "datomic:mem://")

(defn scratch-conn
  "Create a connection to an anonymous, in-memory database."
  []
  (let [uri (str db-uri-base (d/squuid))]
    (d/delete-database uri)
    (d/create-database uri)
    (d/connect uri)))

(defmacro easy!
  "Set up a bunch of REPL conveniences. See the source
   with (source easy!) for details."
  []
  '(do
     #_(set! *warn-on-reflection* true)
     (set! *print-length* 20)
     (use 'datomic.samples.datalog
          'datomic.samples.io
          'datomic.samples.query
          'datomic.samples.generators
          'datomic.samples.transact
          'datomic.samples.schema
          'clojure.repl
          'clojure.pprint)
     (require
      '[clojure.string :as str]
      '[clojure.java.io :as io]
      '[clojure.pprint :as pprint]
      '[clojure.data.generators :as gen]
      '[datomic.api :as d])
     :awesome))

(defmacro defpp
  "Like def, but pretty prints the value of the var created"
  [name & more]
  `(do
     (def ~name ~@more)
     (pprint/pprint ~name)
     (var ~name)))

(defn transcript
  "Run all forms, printing a transcript as if forms were
   individually entered interactively at the REPL."
  [forms]
  (binding [*ns* *ns*]
    (let [temp (gensym)]
      (println ";; Executing forms in temp namespace: " temp)
      (in-ns temp)
      (clojure.core/use 'clojure.core 'clojure.repl 'clojure.pprint)
      (doseq [f forms]
        (pprint/pprint f)
        (print "=> ")
        (pprint/pprint (eval f))
        (println))
      (remove-ns temp)
      :done)))

(defmacro should-throw
  "Runs forms, expecting an exception. Prints descriptive message if
   an exception occurred. Throws if an exception did *not* occur."
  [& forms]
  `(try
    ~@forms
    (throw (ex-info "Expected exception" {:forms '~forms}))
    (catch Throwable t#
      (println "Got expected exception:\n\t" (.getMessage t#)))))
