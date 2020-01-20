(ns meta-schema.core
  (:require [clojure.edn :as edn]
            [clojure.string :as cstr]
            [spec-tools.data-spec :as ds]))

(defn- load-specs [filename]
  (let [single-spec (->> filename
                         slurp
                         edn/read-string)]
    (->> (for [k (keys single-spec)]
           (hash-map k (get-in single-spec [k :location])))
         (into {}))))

(def available-specs (atom {}))

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
    (let [spec-name (:spec-name map-spec)
          spec-gen (traverse-file-spec (dissoc map-spec :spec-name))]
      (ds/spec {:name spec-name
                :spec spec-gen}))))
