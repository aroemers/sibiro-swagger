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
