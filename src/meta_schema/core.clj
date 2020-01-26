(ns meta-schema.core
  (:require [clojure.edn :as edn]
            [clojure.string :as cstr]
            [spec-tools.data-spec :as ds]
            [meta-schema.dsl :refer :all :as dsl]
            [clojure.spec.alpha :as s]
            [clojure.walk :as walk]
            [clojure.java.io :as io]))

(def available-specs
  "All the available specs loaded by the system through the `setup!` function."
  (atom {}))

(defn- load-specs [filename]
  "If the file is not complaint with the usage of `:intent` and `:location`
a exception will be raised."
  (let [single-spec (->> filename
                         slurp
                         edn/read-string)]
    (->> (for [k (keys single-spec)]
           (if-not (s/valid? ::dsl/setup-file (get single-spec k))
             (throw (ex-info "Config file is out of specification format" (get single-spec k)))
             (hash-map k (get-in single-spec [k :location]))))
         (into {}))))

(defn setup! [files]
  "Perform the configuration of your pre-defined specs.

  :files   a list of java.io.File objects with the definitions
of your pre-defined specs. You should follow the specified documentation
about the content type of this files e.g. required keys `:intent` and
`:location`."
  (let [definitions (->> files
                         (filter #(.isFile %))
                         (map load-specs)
                         (into {}))]
    (if (empty? definitions)
      (throw (ex-info "No definitions found." {}))
      (swap! available-specs conj definitions))))


(defn- spec-name--or-clause [field-name spec-name]
  (let [fname (name field-name)
        sname (name spec-name)]
    (keyword (str fname "-" sname))))

(defn- spec-name--map-in-vector [nodes]
  (->> (keys nodes)
       (map name)
       (cstr/join "-")))

(def pre-targets (atom {}))

(defn- traverse-file-spec
  ([spec]
   (loop [k (keys spec)
          parser {}]
     (if (empty? k)
       parser
       (recur (rest k)
              (conj parser
                    (let [node (get spec (first k))
                          field-name (first k)]
                      (hash-map (if (:optional? node)
                                  (ds/opt field-name)
                                  field-name)
                                (cond
                                  ;; first level, already has a :spec keyword defining the data coercion
                                  (:spec node)
                                  (do
                                    (swap! pre-targets assoc field-name (:destination node))
                                    (if (:nullable? node)
                                      (ds/maybe (get @available-specs (:spec node)))
                                      (get @available-specs (:spec node))))

                                  ;; second level, we get a vector with a map containing :spec inside it.
                                  (and (vector? node) (:spec (first node)))
                                  (do
                                    (swap! pre-targets assoc field-name (:destination (first node)))
                                    (if (> (count node) 1)
                                      [(ds/or
                                        (->> (for [n node]
                                               (if (:spec n)
                                                 (hash-map (spec-name--or-clause field-name (:spec n)) (get @available-specs (:spec n)))
                                                 (hash-map (spec-name--or-clause field-name (spec-name--map-in-vector n)) (traverse-file-spec n))))
                                             (into {})))]
                                      [(get @available-specs (:spec (first node)))]))

                                  ;; third level, we get a vector with a new nested map inside, so call it again.
                                  (and (vector? node) (nil? (:spec (first node))))
                                  [(traverse-file-spec (first node))]

                                  ;; fourth level, we get a nested structure map without a :spec key to define the coercion
                                  :else (traverse-file-spec node))))))))))

(defn create-parser [map-spec]
  (if (empty? @available-specs)
    (throw (ex-info "You need to setup! your spec definitions first!" {}))
    (let [spec-name (:spec-name map-spec)
          spec-gen (traverse-file-spec (dissoc map-spec :spec-name))]
      (ds/spec {:name spec-name
                :spec spec-gen}))))

(defn- find-key [data ks matched-keys]
  (walk/walk (fn [[k v]]
               (cond
                 (and (contains? (set ks) k) (not (contains? (set @matched-keys) k))) (do (swap! matched-keys conj k) [k v])
                 (map? v) (find-key v ks matched-keys)
                 :else (into {} (map #(find-key % ks matched-keys) v)))) identity data))

(defn find-keys [data ks]
  (let [matched-keys (atom [])]
    (find-key data ks matched-keys)))


(defn input-data>target-data
  "Convert an input json file to an output json file based on specifications.

  :data           INPUT data
  :data-spec      Map to describe the INPUT data and the desired TARGET location of each key through `:dest` parameters
  :target-shape   Map to describe the OUTPUT data format based on the `:dest` keyword placed in the `:spec-data`

  If the :target-fmt has a placeholder that was not
  fulfilled by the spec-data, it will return the placeholder
  instead of the desired data at that place"
  [data data-spec target-shape]
  (let [parser (create-parser data-spec)
        valid? (s/valid? parser data)]
    (if-not valid?
      (s/explain-data parser data)
      (-> @pre-targets
          (walk/postwalk-replace data)
          (find-keys (vals @pre-targets))
          (walk/postwalk-replace target-shape)))))


(comment

  (require '[clojure.java.io :as io])
  ;; TODO: Remover o atom de pre-targets do codigo e gerar essa variavel de outra forma
  ;; TODO: processo de post-walk está correto, porém vale a pena explorar edge cases com nomes duplicados e etc...

  (def spec-escrita-por-PO {:spec-name ::testando
                            :valor {:spec :teste
                                    }
                            :vals [{:spec :teste
                                    :dest :l}]
                            :valsb {:life {:casa {:spec :teste
                                                  :dest :x}}}})

  (def data-original {:valor 20
                      :vals [20 30 40 50]
                      :valsb {:life {:casa 99}}})

  (def target-fmt-conhecido {:new-val :v
                             })

  (setup! (-> (io/resource "specs")
              (io/file)
              (file-seq)))

  (def client-data {:zip ["101030-201", "987621-281"]
                    :rent 980.322
                    :university {:departments [{:zip {:address "University at Medium Inc.,"}}]}})

  (def client-spec {:spec-name :my-project.client/payload
                    :zip [{:spec :zipcode
                           :optional? false
                           :destination :zipcode
                           :nullable? false}]

                    :rent {:spec :money
                           :optional? false
                           :destination :value
                           :nullable? false}

                    :university {:departments [{:zip {:address {:spec :zipcode
                                                                :destination :university-address
                                                                :optional? false}}}]}})

  (def target-fmt {:my-internal-zipcode :zipcode
                   :my-internal-value   :value
                   :my-internal-address :university-address})

  (setup! (-> (io/resource "specs")
              (io/file)
              (file-seq)))

  (input-data>target-data client-data client-spec target-fmt)

  )
