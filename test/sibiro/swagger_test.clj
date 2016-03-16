(ns sibiro.swagger-test
  (:require [clojure.test :refer :all]
            [sibiro.swagger :refer :all]))

(deftest test-basic-route
  (let [routes #{[:get "/basic"]}]
    (is (= {"/basic" {:get {} :parameters ()}}
           (:paths (swaggerize routes))))))

(deftest test-parameterized-route
  (let [routes #{[:get "/basic/:id/*"]}]
    (is (= {"/basic/{id}/{*}" {:get {} :parameters [{:name :id :type :string :in :path}
                                                    {:name :* :type :string :in :path}]}}
           (:paths (swaggerize routes))))))

(deftest test-parameter-at-start
  (let [routes #{[:get "*"]}]
    (is (= {"/{*}" {:get {} :parameters [{:name :* :type :string :in :path}]}}
           (:paths (swaggerize routes))))))

(deftest test-base
  (let [routes #{[:get "/basic"]}]
    (is (= {:paths {"/basic" {:get {} :parameters ()}}
            :info {:title "My test title"
                   :version "0.1-SNAPSHOT"}
            :swagger "2.0"}
           (swaggerize routes :base {:info {:title "My test title"}})))))

(deftest test-route-info
  (let [routes #{[:get "/basic" {:swag {:description "My basic route"}}]}]
    (is (= {"/basic" {:get {:description "My basic route"}
                      :parameters ()}}
           (:paths (swaggerize routes :route-info :swag))))))
