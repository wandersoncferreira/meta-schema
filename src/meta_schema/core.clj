(ns meta-schema.core
  (:require [clojure.spec.alpha :as s]
            [spec-tools.core :as ds]
            [clojure.string :as cstr]))

(defn- spec-name--or-clause [field-name spec-name]
  (let [fname (name field-name)
        sname (name spec-name)]
    (keyword (str fname "-" sname))))

(defn- spec-name--map-in-vector [nodes]
  (->> (keys nodes)
       (map name)
       (cstr/join "-")))

(defn prepare-parser
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
                                      (ds/maybe (get available-specs (:spec node)))
                                      (get available-specs (:spec node)))

                                    (and (vector? node) (:spec (first node)))
                                    (if (> (count node) 1)
                                      [(ds/or
                                        (->> (for [n node]
                                               (if (:spec n)
                                                 (hash-map (spec-name--or-clause field-name (:spec n)) (get available-specs (:spec n)))
                                                 (hash-map (spec-name--or-clause field-name (spec-name--map-in-vector n)) (prepare-parser n))))
                                             (into {})))]
                                      [(get available-specs (:spec (first node)))])

                                    (and (vector? node) (nil? (:spec (first node))))
                                    [(prepare-parser (first node))]

                                    :else (prepare-parser node)))))))))))

(comment

  (s/def ::cnpj int?)
  (s/def ::numero-casa int?)
  (s/def ::letras string?)

  (def available-specs
    {:cnpj ::cnpj
     :numero ::numero-casa
     :letras ::letras})

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

  (def payload-spec
    (ds/spec
     {:name ::payload
      :spec (prepare-parser file-spec)}))


  (s/valid? payload-spec {:valores [{:treta nil}]
                          :celular 20
                          :bairro {:numero [{:letreiro {:agora "1312"}}]
                                   :federal 30}
                          :casa 30}))
