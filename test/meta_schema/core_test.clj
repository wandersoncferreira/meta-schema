(ns meta-schema.core-test
  (:require [clojure.test :refer :all]
            [meta-schema.core :as ms]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]))

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

(gen/sample spec-file-gen 2)


(s/def ::money (s/or :int int?
                     :float float?
                     :double double?))

(s/def ::zipcode string?)
(s/def ::life (s/and int? #(= 42 %)))
(s/def ::address string?)

(deftest simple-file-spec
  (ms/setup! (-> (io/resource "specs")
                 (io/file)
                 (file-seq)))
  (let [file-spec {:spec-name :meta-schema.core-test/testing
                   :cep {:spec :zipcode}
                   :amount {:spec :money}}

        parser (ms/create-parser file-spec)]
    (is (s/valid? parser {:cep "brazilian cep"
                          :amount 20}))
    (is (not (s/valid? parser {:cep 10
                               :amount 20})))))


(defspec cannot-break-to-nested-fns 400
  (prop/for-all [file-spec spec-file-gen]
                (ms/setup! (-> (io/resource "specs")
                               (io/file)
                               (file-seq)))
                (= (spec-tools.core/spec? (ms/create-parser file-spec)))))
