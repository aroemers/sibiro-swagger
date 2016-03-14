(ns sibiro.swagger
  "Transform routes to a Swagger 2.0 spec."
  (:require [cheshire.core :as json]
            [clojure.string :as str]))

(defn parameters [params]
  (for [param params]
    {:name param
     :in "path"
     :type "string"}))

(defn path [uri]
  (let [no-regexes (str/replace uri #"\{.+?\}" "")
        params-re  #"/(?::(.+?)|\*)(?=/|$)"
        params     (->> (re-seq params-re no-regexes)
                        (map (fn [[_ p]] (if p (keyword p) :*))))]
    [(str/replace no-regexes params-re (fn [[_ p]] (str "/{" (or p "*") "}")))
     params]))

(defn- paths [routes path-info]
  (loop [routes        routes
         swagger-paths {}]
    (if-let [[method uri handler] (first routes)]
      (let [[swagger-path params] (path uri)
            swagger-parameters    (parameters params)
            swagger-info          (when path-info (path-info handler))
            swagger-path-info     (merge-with merge {:parameters swagger-parameters} swagger-info)]
        (if (= method :any)
          (recur (rest routes)
                 (reduce (fn [sps mth] (update sps swagger-path assoc mth swagger-path-info))
                         swagger-paths [:get :put :post :delete :options :head :patch]))
          (recur (rest routes)
                 (update swagger-paths swagger-path assoc method swagger-path-info))))
      swagger-paths)))

(defn swaggerize
  "Generate a Clojure map holding a Swagger specification based on the
  given (uncompiled) routes. Current options are:

  :base - A map that is merged in with the generated swagger root
  object. You can for instance set a title for your API by calling:
  (swaggerize [...] :base {:info {:title \"My asewome API\"}}).

  :path-info - A function that gets each route handler as its
  argument. Its result is merged with the operation object. Default is
  (fn [h] (when (map? h) (:swagger h)))."
  [routes & {:keys [base path-info]
             :or (path-info (fn [h] (when (map? h) (:swagger h))))}]
  (merge base-spec
         (merge-with merge {:swagger "2.0"
                            :info {:title "I'm too lazy to name my API"
                                   :version "0.1-SNAPSHOT"}
                            :paths (paths routes path-info)}
                     base)))

(defn swaggerize-json
  "Same as `swaggerize`, but generates a JSON string."
  [routes & opts]
  (json/generate-string (apply swaggerize routes opts)))
