(ns meta-schema.core-test
  (:require [clojure.test :refer :all]
            [meta-schema.core :as ms]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]))

(s/def ::money (s/or :int int?
                     :float float?
                     :double double?))

(s/def ::zipcode string?)

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

(def file-spec {:spec-name :my-project.client/payload
                :zip [{:spec :zipcode
                       :optional? false
                       :nullable? false}

                      {:house-price {:spec :money
                                     :nullable? true}}]

                :rent {:spec :money
                       :optional? false
                       :nullable? false}

                :university {:departments [{:zip {:address {:spec :zipcode
                                                            :optional? false}}}]}})
