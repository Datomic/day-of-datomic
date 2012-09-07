;   Copyright (c) Metadata Partners, LLC. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns datomic.samples.repl
  (:use [datomic.api :only (q db) :as d])
  (:require [clojure.pprint :as pprint]))

(defn scratch-conn
  "Create a connection to an anonymous, in-memory database."
  []
  (let [uri (str "datomic:mem://" (d/squuid))]
    (d/delete-database uri)
    (d/create-database uri)
    (d/connect uri)))

(defn tempid
  "Create a tempid in the :db.part/user partition."
  ([] (d/tempid :db.part/user))
  ([n] (d/tempid :db.part/user n)))

(defmacro easy!
  "Set up a bunch of REPL conveniences. See the source
   with (source easy!) for details."
  []
  `(do
     #_(set! *warn-on-reflection* true)
     (set! *print-length* 20)
     (use '[datomic.api :only (~'q ~'db) :as ~'d]
          'datomic.samples.datalog
          'datomic.samples.io
          'datomic.samples.query
          'datomic.samples.generators
          'datomic.samples.transact
          'datomic.samples.incanter)
     (require
      '[clojure.string :as ~'str]
      '[clojure.java.io :as ~'io]
      '[clojure.pprint :as ~'pprint]
      '[clojure.test.generative.generators :as ~'gen])
     :awesome))

(defmacro defpp
  "Like def, but pretty prints the value of the var created"
  [name & more]
  `(do
     (def ~name ~@more)
     (pprint/pprint ~name)
     (var ~name)))


