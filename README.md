# meta-schema

[![Clojars Project](https://img.shields.io/clojars/v/meta-schema.svg)](https://clojars.org/meta-schema)


Designed to help data processing using `clojure.spec`.

Provide users the ability to combine several pre-defined
`clojure.spec` into some specific shape at *runtime*.


For now, it only supports JSON files.

## Installation

Leiningen/Boot

```clj
[meta-schema "0.1.1"]
```

Clojure CLI/deps.edn
```clj
meta-schema {:mvn/version "0.1.1"}
```

## Usage

In your project you need to define EDN files inside a folder
and provide a list of `java.io.File`s to the `(setup!  ..)`
function. These files are simple and provides the location
and intent of your pre-defined specs.

```clj
{:zipcode {:intent "Validation to ZipCodes in Brazil"
           :location :my-project.spec/zipcode-br}
 :money {:intent "How we understand money around here? Anything >= 0 either long, float, double, or decimal"
         :location :my-project.other-spec/money-br}
}
```

Make sure that all this namespaces are loaded before invoke
`meta-schema.core/setup!`.


Example of specification provided to the library at runtime.

```clj
  (require '[meta-schema.core :as ms])
  (require '[clojure.alpha.spec :as s])
  (require '[clojure.java.io :as io])

  (ms/setup! (-> (io/resources "specs")
                 (io/file)
                 (file-seq)))

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

  (def my-parser (ms/create-parser file-spec))

  (def my-data-1 {:zip ["my zipcode is text"]
                  :rent 10
                  :university {:departments [{:zip {:address "deep structures"}}]}})

  (s/valid? my-parser my-data-1) ;; => true

  (s/valid? my-parser {:zip [{:house-price 230} "either or both here.."]
                       :rent 10
                       :university {:departments [{:zip {:address "deep structures"}}]}}) ;; => true

```

## Rational

The DSL is simple:

1. When the field is a list, more than one spec inside it will produce a `or` operator
2. When the field is `optional`, it means that  may or may not be present at coersion time
3. When the field is `nullable`, it means that `nil` is a valid input.
4. By default all the fields are *not* nullable and *not* optionals.
5. You can nest the options as you desired


## TODO list

- [ ] Improve interfaces of public API
- [ ] Write a parser to spec the DSL in the input-file
- [ ] Provide support for `sets` and `enums` (?)
- [ ] Extend it to XML

## License

Copyright © 2020 Wanderson Ferreira (@bartuka)

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
