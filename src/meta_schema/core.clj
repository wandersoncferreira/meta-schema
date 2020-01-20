(ns meta-schema.core
  (:require [clojure.spec.alpha :as s]
            [spec-tools.data-spec :as ds]
            [clojure.string :as cstr]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(s/def ::cnpj int?)
(s/def ::numero int?)
(s/def ::letras string?)

(defn- load-specs [filename]
  (->> filename
       slurp
       edn/read-string))

(def available-specs (atom {}))

(defn setup! [resource-folder]
  "Provide the folder where all your specs are registered."
  (let [definitions (->> resource-folder
                         (io/resource)
                         (io/file)
                         (file-seq)
                         (filter #(.isFile %))
                         (map load-specs)
                         (into {}))]
    (swap! available-specs conj definitions)))



(defn- spec-name--or-clause [field-name spec-name]
  (let [fname (name field-name)
        sname (name spec-name)]
    (keyword (str fname "-" sname))))

(defn- spec-name--map-in-vector [nodes]
  (->> (keys nodes)
       (map name)
       (cstr/join "-")))

(defn- traverse-file-spec
  ([spec]
   (let [fkeys (keys spec)]
     (loop [k fkeys
            parser {}]
       (if (empty? k)
         parser
         (recur (rest k)
                (conj parser
                      (let [node (get spec (first k))
                            field-name (first k)]
                        (hash-map (cond
                                    (:optional? node) (ds/opt field-name)
                                    :else             field-name)
                                  (cond
                                    (:spec node)
                                    (if (:nullable? node)
                                      (ds/maybe (get @available-specs (:spec node)))
                                      (get @available-specs (:spec node)))

                                    (and (vector? node) (:spec (first node)))
                                    (if (> (count node) 1)
                                      [(ds/or
                                        (->> (for [n node]
                                               (if (:spec n)
                                                 (hash-map (spec-name--or-clause field-name (:spec n)) (get @available-specs (:spec n)))
                                                 (hash-map (spec-name--or-clause field-name (spec-name--map-in-vector n)) (traverse-file-spec n))))
                                             (into {})))]
                                      [(get @available-specs (:spec (first node)))])

                                    (and (vector? node) (nil? (:spec (first node))))
                                    [(traverse-file-spec (first node))]

                                    :else (traverse-file-spec node)))))))))))

(defn create-parser [map-spec]
  (if (empty? @available-specs)
    (throw (ex-info "You need to setup! your spec definitions first!" {}))
    (let [spec-gen (traverse-file-spec map-spec)]
      (ds/spec {:name ::payload
                :spec spec-gen}))))

(comment

  (def file-spec {:valores [{:spec :cnpj
                             :optional? false
                             :nullable? false}
                            {:treta {:spec :letras
                                     :nullable? true}
                             :total {:spec :numero
                                     :optional? true}}]

                  :celular {:spec :cnpj
                            :optional? true
                            :nullable? true}

                  :bairro {:numero [{:letreiro {:agora {:spec :letras
                                                        :optional? true}}}]
                           :federal {:spec :cnpj
                                     :optional? false}}
                  :casa {:spec :numero
                         :optional? true}})

  (setup! "specs/teste.edn")
  (def teste-spec (create-parser file-spec))

  (def payload-spec
    (ds/spec
     {:name ::payload
      :spec (traverse-file-spec file-spec)}))


  (s/valid? payload-spec {:valores [{:treta nil}]
                          :celular 20
                          :bairro {:numero [{:letreiro {:agora "1312"}}]
                                   :federal 30}
                          :casa 30})
  )
