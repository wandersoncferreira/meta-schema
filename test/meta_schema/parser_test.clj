(ns meta-schema.parser-test
  (:require [clojure.test :refer :all]
            [meta-schema.core :as ms]
            [spec-tools.core :as sc]
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
                   :amounts [{:spec :money}]}

        parser (ms/create-parser file-spec)]
    (is (s/valid? parser {:cep "brazilian cep"
                          :amounts [20 320 40 50]}))
    (is (not (s/valid? parser {:cep 10
                               :amounts 20})))))

(deftest failing-specs
  (ms/setup! (-> (io/resource "specs")
                 (io/file)
                 (file-seq)))
  (let [file-spec {:spec-name :meta-schema.core-test/test-fail
                   :cep {:spec :zipcode}
                   :address {:cep {:spec :zipcode}}
                   :money {:spec :money}}
        parser (ms/create-parser file-spec)]

    (is (not (s/valid? parser {:cep "br"
                               :address {:cep 10.0}
                               :money 20})))))


(defspec cannot-break-to-nested-fns 400
  (prop/for-all [file-spec spec-file-gen]
                (ms/setup! (-> (io/resource "specs")
                               (io/file)
                               (file-seq)))
                (= (sc/spec? (ms/create-parser file-spec)))))
