# sibiro-swagger

Generate a [swagger](http://swagger.io) specification for your [sibiro](https://github.com/aroemers/sibiro) routes.
It generates a specification according to [OpenAPI specification 2.0](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md).

## Usage

Add `[functionalbytes/sibiro-swagger "0.1.4"]` to your dependencies.

The library offers basically one function: `swaggerize`.
It takes your (uncompiled) routes datastructure, and returns a Clojure datastructure holding the specification.
It also takes additional keyword arguments.
Currently the following are supported:

- `:base` - A map that is deep merged in with the generated swagger root object.
  You can for instance set a title for your API by calling:
  ```clj
  (swaggerize [...]
    :base {:info {:title "My asewome API"}})
  ```

- `:route-info` - A function that gets each route handler as its argument.
  Its result is merged with the operation object.
  The default is `#(when (map? %) (:swagger %))`, so you _could_ define routes like:
  ```clj
  [[:get "/user/:id" {:handler get-user
                      :swagger {:description "Get user by ID"}}]]
  ```
  If you find that this clutters your route definitions, have a look at the more intricate [example](EXAMPLE.md).

- `:param-level` - A keyword indicating where the default parameters should be placed.
  If set to `:path`, the parameters are defined on the path level.
  If set to `:operation`, the parameters are defined on the operation level.
  If set to `:both`, the parameters are defined on the path level, and at the operation level those are referenced.
  Default is `:path`.

  This option is mainly for circumventing that some Swagger UI versions need parameters on the operation level, and other versions don't get the overriding of parameters right.

There are also two functions called `swaggerize-json` and `swaggerize-yaml`, which is the same as `swaggerize`, but returns a JSON or YAML string.

### An example

Let's have two simple routes, one with some extra route info:

```clj
(def routes [[:get  "/user/:id" {:swagger {:description "Get user by ID"
                                           :parameters [{:name :id :type :integer
                                                         :in :path :required true}]
                                           :responses {200 {:description "User found"}}}}]
             [:post "/user/:id" nil])

(swaggerize-yaml routes :base {:info {:version "0.3"}})
```

Above results in the following YAML. Note that some defaults are still visibile, such as the title and the responses of the POST operation.

```yaml
swagger: '2.0'
info:
  title: I'm too lazy to name my API
  version: '0.3'
paths:
  /user/{id}:
    get:
      responses:
        '200':
          description: User found
      parameters:
      - name: id
        type: integer
        in: path
        required: true
      description: Get user by ID
    post:
      responses:
        default:
          description: Default response.
    parameters:
    - name: id
      in: path
      type: string
      required: true
```

_As always, have fun!_

## Contributing

Patches and ideas welcome.

Master branch: [![Circle CI](https://circleci.com/gh/aroemers/sibiro-swagger/tree/master.svg?style=svg)](https://circleci.com/gh/aroemers/sibiro-swagger/tree/master)

## License

Copyright © 2016 Functional Bytes

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
