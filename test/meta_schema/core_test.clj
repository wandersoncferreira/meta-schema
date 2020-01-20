(ns meta-schema.core-test
  (:require [clojure.test :refer :all]
            [meta-schema.core :as ms]
            [clojure.spec.alpha :as s]))

(s/def ::money (s/or :int int?
                     :float float?
                     :double double?))

(s/def ::zipcode string?)


(ms/setup! "specs")

(deftest simple-file-spec
  (let [file-spec {:cep {:spec :zipcode}
                   :amount {:spec :money}}
        parser (ms/create-parser file-spec)]
    (is (s/valid? parser {:cep "brazilian cep"
                          :amount 20}))
    (is (not (s/valid? parser {:cep 10
                               :amount 20})))))
