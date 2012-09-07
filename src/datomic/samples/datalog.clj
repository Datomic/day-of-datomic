(ns datomic.samples.datalog)

(defn tuplify
  "Returns a vector of the vals at keys ks in map."
  [m ks]
  (mapv #(get m %) ks))

(defn maps->rel
  "Convert a collection of maps into a relation, returning
   the tuplification of each map by ks"
  [maps ks]
  (mapv #(tuplify % ks) maps))

