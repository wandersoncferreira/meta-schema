(ns meta-schema.dsl
  (:require [clojure.spec.alpha :as s]))


(s/def ::spec keyword?)
(s/def ::optional? boolean?)
(s/def ::nullable? boolean?)

(s/def ::options (s/keys :req-un [::spec]
                         :opt-un [::optional?
                                  ::nullable?]))
