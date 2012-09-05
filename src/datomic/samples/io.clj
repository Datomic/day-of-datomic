;   Copyright (c) Metadata Partners, LLC. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns datomic.samples.io
  (:use [datomic.api :only (q db) :as d])
  (:require [clojure.java.io :as jio])
  (:import datomic.Util))

(defn resource-uri
  "Returns URI or nil"
  [r]
  (let [cl (.getContextClassLoader (Thread/currentThread))]
    (.getResource cl r)))

(defn read-all
  "Read all forms in f"
  [f]
  (Util/readAll (jio/reader f)))

(defn transact-all
  "Load and run all transactions from f"
  [conn f]
  (doseq [txd (read-all f)]
    (d/transact conn txd))
  :done)
