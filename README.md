# sibiro-swagger

Generate a [swagger](http://swagger.io) specification for your [sibiro](https://github.com/aroemers/sibiro) routes.
It generates a specification according to [OpenAPI specification 2.0](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md).

## Usage

Add `[functionalbytes/sibiro-swagger "0.1.4"]` to your dependencies.

The library offers basically one function: `swaggerize`.
It takes your (uncompiled) routes datastructure, and returns a Clojure datastructure holding the specification.
It also takes additional keyword arguments.
Currently the following are supported:

- `:base` - A map that is merged in with the generated swagger root object.
  You can for instance set a title for your API by calling:
  ```clj
  (swaggerize [...]
    :base {:info {:title "My asewome API"}})
  ```

- `:route-info` - A function that gets each route handler as its argument.
  Its result is merged with the operation object.
  The default is `(fn [h] (when (map? h) (:swagger h)))`, so you _could_ define routes like:
  ```clj
  [[:get "/user/:id" {:handler get-user
                      :swagger {:description "Get user by ID"}}]]
  ```

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

## Example with external swagger path info

You may find that defining swagger route information inside the routes datastructure is cluttering it.
Of course there are many more ways of retrieving that route information.
Below is an example of it could be done, using multi-methods and a macro.

First we define the multi-methods and the macro.

```clj
(defmulti route-info   identity)
(defmulti route-handle :route-handler)

(defmacro defhandler [keyword swagger [request] & body]
  `(do (defmethod route-info ~keyword [~'_] ~swagger)
       (defmethod route-handle ~keyword [~request] ~@body)))
```

Now we can define our routes using just a keyword as the handler object.

```clj
(def my-routes
  [[:get "/user/:id" :user-get]
   ...])
```

Defining a handler that nicely includes the swagger information, can now be done as follows:

```clj
(defhandler :user-get
  {:description "Get user by ID"
   :parameters [{:name :id, :type :integer, :in :path, :required true}]}
  [{{id :id} :route-params}]
  {:status 200
   :body (db/user-by-id id)})
```

And lastly, the following function returns the swagger specification for the routes.

```clj
(defn my-swaggerize []
  (swaggerize my-routes
    :base {:info {:title "My awesome API"}}
    :route-info route-info))
```

_As always, have fun!_

## Contributing

Patches and ideas welcome.

Master branch: [![Circle CI](https://circleci.com/gh/aroemers/sibiro-swagger/tree/master.svg?style=svg)](https://circleci.com/gh/aroemers/sibiro-swagger/tree/master)

## License

Copyright © 2016 Functional Bytes

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
