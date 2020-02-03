(defproject meta-schema "0.1.4"
  :description "DSL to write data processing schemas faster"
  :url "https://github.com/wandersoncferreira/meta-schema"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [metosin/spec-tools "0.10.1"]
                 [org.clojure/test.check "0.10.0"]]
  :profiles {:dev {:plugins [[jonase/eastwood "0.3.5"]
                             [lein-tach "1.0.0"]
                             [lein-doo "0.1.11"]
                             [lein-cljsbuild "1.1.7"]
                             [lein-cloverage "1.1.1"]
                             [lein-codox "0.10.7"]]}}
  :aliases {"all" ["with-profile" "dev"]
            "test-clj" ["all" "do" ["test"] ["check"]]}
  :repl-options {:init-ns meta-schema.core})
