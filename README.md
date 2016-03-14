# sibiro-swagger

Generate a [swagger-ui](https://github.com/swagger-api/swagger-ui) or [swagger-codegen](https://github.com/swagger-api/swagger-codegen) specification.
It generates a specification according to [OpenAPI specification 2.0](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md).

## Usage

Add `[functionalbytes/sibiro-swagger "0.1.0"]` to your dependencies.

The library offers basically one function: `swaggerize`.
It takes your (uncompiled) routes datastructure, and returns a Clojure datastructure holding the specification.
It also takes additional keyword arguments.
Currently the following are supported:

- `:base` - A map that is merged in with the generated swagger root object.
  You can for instance set a title for your API by calling:
  ```clj
  (swaggerize [...]
    :base {:info {:title \"My asewome API\"}})
  ```

- `:path-info` - A function that gets each route handler as its argument.
  Its result is merged with the operation object.
  The default is `(fn [h] (when (map? h) (:swagger h)))`, so you could define routes like:
  ```clj
  [[:get "/user/:id" {:handler get-user
                      :swagger {:description "Get user by ID"}}]]
  ```

## License

Copyright © 2016 Functional Bytes

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
