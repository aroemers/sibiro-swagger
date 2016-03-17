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

(defn- parameters [params]
  (for [param params]
    {:name param
     :in :path
     :type :string}))

(defn- clean-uri [uri]
  (let [;; remove regexes
        cleaned (str/replace uri #"\{.+?\}" "")
        ;; ensure slash at beginning
        cleaned (cond->> cleaned (not= (first cleaned) \/) (str \/))]
    cleaned))

(defn- path [uri]
  (let [cleaned (clean-uri uri)
        params-re  #"/(?::(.+?)|\*)(?=/|$)"
        params     (->> (re-seq params-re cleaned)
                        (map (fn [[_ p]] (if p (keyword p) :*))))]
    [(str/replace cleaned params-re (fn [[_ p]] (str "/{" (or p "*") "}")))
     params]))

(defn- paths [routes route-info]
  (loop [routes        routes
         swagger-paths {}]
    (if-let [[method uri handler] (first routes)]
      (let [[swagger-path params]  (path uri)
            swagger-parameters     (or (get-in swagger-paths [swagger-path :parameters])
                                       (parameters params))
            swagger-operation-info (or (route-info handler) {})]
        (if (= method :any)
          (recur (rest routes)
                 (reduce (fn [sps mth]
                           (-> sps
                               (update swagger-path assoc mth swagger-operation-info)
                               (update swagger-path assoc :parameters swagger-parameters)))
                         swagger-paths [:get :put :post :delete :options :head :patch]))
          (recur (rest routes)
                 (-> swagger-paths
                     (update swagger-path assoc method swagger-operation-info)
                     (update swagger-path assoc :parameters swagger-parameters)))))
      swagger-paths)))

(defn swaggerize
  "Generate a Clojure map holding a Swagger specification based on the
  given (uncompiled) routes. Current options are:

  :base - A map that is merged in with the generated swagger root
  object. You can for instance set a title for your API by calling:
  (swaggerize [...] :base {:info {:title \"My asewome API\"}}).

  :route-info - A function that gets each route handler as its
  argument. Its result is merged with the operation object. Default is
  (fn [h] (when (map? h) (:swagger h)))."
  [routes & {:keys [base route-info]
             :or {route-info (fn [h] (when (map? h) (:swagger h)))}}]
  (deep-merge {:swagger "2.0"
               :info {:title "I'm too lazy to name my API"
                      :version "0.1-SNAPSHOT"}
               :paths (paths routes route-info)}
              base))

(defn swaggerize-json
  "Same as `swaggerize`, but generates a JSON string."
  [routes & opts]
  (json/generate-string (apply swaggerize routes opts)))

(defn swaggerize-yaml
  "Same as `swaggerize`, but generates a YAML string."
  [routes & opts]
  (yaml/generate-string (apply swaggerize routes opts) :dumper-options {:flow-style :block}))
