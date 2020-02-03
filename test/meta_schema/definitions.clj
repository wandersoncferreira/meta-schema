(ns meta-schema.definitions
  (:require [clojure.spec.alpha :as s]
            [clojure.test.check.generators :as gen]))


(def spec-gen (gen/hash-map
               :spec (gen/elements [:zipcode :money :life :address])))

(def optional-gen (gen/hash-map
                   :optional? gen/boolean))

(def nullable-gen (gen/hash-map
                   :nullable? gen/boolean))


(def leaf-spec (gen/let [specs spec-gen
                         options (gen/frequency [[6 optional-gen]
                                                 [4 (gen/return {})]])
                         nulls (gen/frequency [[6 nullable-gen]
                                               [4 (gen/return {})]])]
                 (merge specs options nulls)))

(def spec-file-gen (gen/hash-map
                    :spec-name gen/keyword-ns
                    :tk1 (gen/not-empty (gen/vector leaf-spec))
                    :tk2 leaf-spec))


(s/def ::money (s/or :int int?
                     :float float?
                     :double double?))

(s/def ::zipcode string?)
(s/def ::life (s/and int? #(= 42 %)))
(s/def ::address string?)
