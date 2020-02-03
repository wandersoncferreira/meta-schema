(ns meta-schema.reshape-data-test
  (:require  [clojure.test :refer [deftest is testing]]
             [meta-schema.core :as ms]
             [clojure.java.io :as io]))

(deftest simple-reshape
  (let [client-data {:zip ["101030-201", "987621-281"]
                     :rent 980.322
                     :university {:departments [{:zip {:address "University at Medium Inc.,"}}]}}

        client-spec {:spec-name :my-project.client/payload
                     :zip [{:spec :zipcode
                            :destination :a}]
                     :rent {:spec :money
                            :destination :b}
                     :university {:departments [{:zip {:address {:spec :zipcode
                                                                 :destination :c}}}]}}]
    (ms/setup! (-> (io/resource "specs") io/file file-seq))

    (testing "Verify that a simple reshape of data input to output is valid."
      (is (= {:my-new-zip ["101030-201", "987621-281"]
              :my-new-rent 980.322
              :my-new-address "University at Medium Inc.,"}
             (ms/input-data->target-data client-data client-spec {:my-new-zip :a
                                                                  :my-new-rent :b
                                                                  :my-new-address :c}))))

    (testing "A more complicated target structure"
      (is (= {:my-company {:likes {:very {:nested {:structures ["101030-201", "987621-281"]
                                                   :and {:I {:need 980.322
                                                             :my {:job {:at "University at Medium Inc.,"}}}}}}}}}
             (ms/input-data->target-data client-data client-spec
                                         {:my-company {:likes {:very {:nested {:structures :a
                                                                               :and {:I {:need :b
                                                                                         :my {:job {:at :c}}}}}}}}}))))

    (testing "Unfortunately, we have a edge case where the name of the FIELD in your target spec is the same as a DESTINATION keyword"
      (is (not= {:b "University at Medium Inc.,"}
                (ms/input-data->target-data client-data client-spec {:b :c}))
          (= {980.322 "University at Medium Inc.,"}
             (ms/input-data->target-data client-data client-spec {:b :c}))))))
