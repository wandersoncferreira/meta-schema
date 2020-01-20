# meta-schema

Library designed to help with data processing using `clojure.spec`.

The idea is to provide an user the ability to combine
several pre-coded `clojure.spec` into some specific shape at
*runtime*.


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
  (s/valid? my-parser my-data)

```

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
