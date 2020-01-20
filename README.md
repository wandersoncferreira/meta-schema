# meta-schema

Library designed to help with data processing using `clojure.spec`.

The idea is to provide an user the ability to combine
several pre-coded `clojure.spec` into some specific shape at
*runtime*.



## Usage

```
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
                          :casa 30})
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
