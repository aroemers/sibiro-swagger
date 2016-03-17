(ns sibiro.swagger
  "Transform routes to a Swagger 2.0 spec."
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [yaml.writer :as yaml]))

;;; Helper functions.

(defn- deep-merge [& ms]
  (if (every? #(or (map? %) (nil? %)) ms)
    (apply merge-with deep-merge ms)
    (last ms)))


;;; Swagger implementation.

(def ^:private default-response
  {:default
   {:description "Default response."}})

(defn- swagger-parameters [params]
  (for [param params]
    {:name param
     :in :path
     :type :string
     :required true}))

(defn- clean-uri [uri]
  (let [;; remove regexes
        cleaned (str/replace uri #"\{.+?\}" "")
        ;; ensure slash at beginning
        cleaned (cond->> cleaned (not= (first cleaned) \/) (str \/))]
    cleaned))

(defn- swagger-path [uri]
  (let [cleaned (clean-uri uri)
        params-re  #"/(?::(.+?)|\*)(?=/|$)"
        params     (->> (re-seq params-re cleaned)
                        (map (fn [[_ p]] (if p (keyword p) :*))))]
    [(str/replace cleaned params-re (fn [[_ p]] (str "/{" (or p "*") "}")))
     params]))

(defn- swagger-paths [routes route-info param-level]
  (loop [routes routes
         paths  {}]
    (if-let [[method uri handler] (first routes)]
      (let [[path params]  (swagger-path uri)
            path-params    (or (get-in paths [path :parameters])
                               (swagger-parameters params))
            param-refs     (when (= param-level :both)
                             (let [path-ref (str/replace path "/" "~1")]
                               (for [i (range (count path-params))]
                                 {"$ref" (str "#/paths/" path-ref "/parameters/" i)})))
            info           (route-info handler)
            responses      (or (:responses info) default-response)
            operation-info (deep-merge {:responses responses}
                                       (cond (= param-level :operation) {:parameters path-params}
                                             (= param-level :both)      {:parameters param-refs}
                                             :otherwise                 nil)
                                       info)]
        (if (= method :any)
          (recur (rest routes)
                 (reduce (fn [ps mth]
                           (-> ps
                               (update path assoc mth operation-info)
                               (cond-> (#{:path :both} param-level)
                                 (update path assoc :parameters path-params))))
                         paths [:get :put :post :delete :options :head :patch]))
          (recur (rest routes)
                 (-> paths
                     (update path assoc method operation-info)
                     (cond-> (#{:path :both} param-level)
                       (update path assoc :parameters path-params))))))
      paths)))

(defn swaggerize
  "Generate a Clojure map holding a Swagger specification based on the
  given (uncompiled) routes. Current options are:

  :base - A map that is merged in with the generated swagger root
  object. You can for instance set a title for your API by calling:
  (swaggerize [...] :base {:info {:title \"My asewome API\"}}).

  :route-info - A function that gets each route handler as its
  argument. Its result is merged with the operation object. Default is
  (fn [h] (when (map? h) (:swagger h))).

  :param-level - A keyword indicating where the default parameters
  should be placed. If set to :path, the parameters are defined on the
  path level. If set to :operation, the parameters are defined on the
  operation level. If set to :both, the parameters are defined on the
  path level, and at the operation level those are referenced. Default
  is :path. This option is mainly for circumventing that some Swagger
  UI versions need parameters on the operation level, and other
  versions don't get the overriding of parameters right."
  [routes & {:keys [base route-info param-level]
             :or {route-info (fn [h] (when (map? h) (:swagger h)))
                  param-level :path}}]
  (deep-merge {:swagger "2.0"
               :info {:title "I'm too lazy to name my API"
                      :version "0.1-SNAPSHOT"}
               :paths (swagger-paths routes route-info param-level)}
              base))

(defn swaggerize-json
  "Same as `swaggerize`, but generates a JSON string."
  [routes & opts]
  (json/generate-string (apply swaggerize routes opts)))

(defn swaggerize-yaml
  "Same as `swaggerize`, but generates a YAML string."
  [routes & opts]
  (yaml/generate-string (apply swaggerize routes opts) :dumper-options {:flow-style :block}))
