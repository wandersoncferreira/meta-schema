(ns meta-schema.parser-test
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.test :refer :all]
            [meta-schema.definitions :refer :all]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [meta-schema.core :as ms]
            [spec-tools.core :as sc]))

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
