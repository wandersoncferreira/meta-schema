# meta-schema [![Build Status](https://travis-ci.org/wandersoncferreira/meta-schema.svg?branch=master)](https://travis-ci.org/wandersoncferreira/meta-schema) [![Coverage Status](https://coveralls.io/repos/github/wandersoncferreira/meta-schema/badge.svg?branch=feature/setup-travis)](https://coveralls.io/github/wandersoncferreira/meta-schema?branch=feature/setup-travis)

[![Clojars Project](https://img.shields.io/clojars/v/meta-schema.svg)](https://clojars.org/meta-schema)


Designed to help data processing using `clojure.spec`.

Provide users the ability to combine several pre-defined
`clojure.spec` into some specific shape at *runtime*.


For now, it only supports JSON files. This library is in
alpha-stage, still needs many test cases and refactors,
although the Public API **must** not change.

## Installation

Leiningen/Boot

```clj
[meta-schema "0.1.4"]
```

Clojure CLI/deps.edn
```clj
meta-schema {:mvn/version "0.1.4"}
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

Cool, now we can finally finish the example above. Let's say your source code only understands a specific `target-fmt` of MAP that can be processed. For sure, your clients does not want to send you that format, right? Because they can't change legacy systems and make something better to help everybody. That's ok!

```clj
  (def client-data {:zip ["101030-201", "987621-281"]
                    :rent 980.322
                    :university {:departments [{:zip {:address "University at Medium Inc.,"}}]}})

  (def client-spec {:spec-name :my-project.client/payload
                    :zip [{:spec :zipcode
                           :destination :zipcode}]

                    :rent {:spec :money
                           :destination :value}

                    :university {:departments [{:zip {:address {:spec :zipcode
                                                                :destination :university-address}}}]}})

  (def target-fmt {:my-internal-zipcode :zipcode
                   :my-internal-value   :value
                   :my-internal-address :university-address})

  (setup! (-> (io/resource "specs")
              (io/file)
              (file-seq)))

  (input-data->target-data client-data client-spec target-fmt)

  ;; => {:my-internal-zipcode ["101030-201" "987621-281"],
  ;;     :my-internal-value 980.322,
  ;;     :my-internal-address "University at Medium Inc.,"}
```

The clojure.spec to validate the data is created at runtime
and applied to the provided data. If it passes, we start the
transformation to the `target-shape` specified.

## Rationale

The DSL is simple:

1. When the field is a list, more than one spec inside it will produce a `or` operator
2. When the field is `optional`, it means that  may or may not be present at coersion time
3. When the field is `nullable`, it means that `nil` is a valid input.
4. By default all the fields are *not* nullable and *not* optionals.
5. You can nest the options as you desired


## TODO list

- [x] Finish the second part of the library, `input-data->target-data` conversions
- [x] Improve interfaces of public API
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
