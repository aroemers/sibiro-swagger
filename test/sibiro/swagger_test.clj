(ns sibiro.swagger-test
  (:require [clojure.test :refer :all]
            [sibiro.swagger :refer :all]))

(deftest test-basic-route
  (testing "basic unparameterized route"
    (let [routes #{[:get "/basic"]}]
      (is (= {"/basic" {:get {} :parameters ()}}
             (:paths (swaggerize routes)))))))

(deftest test-parameterized-route
  (testing "basic parameterized route"
    (let [routes #{[:get "/basic/:id/*"]}]
      (is (= {"/basic/{id}/{*}" {:get {} :parameters [{:name :id :type :string :in :path}
                                                      {:name :* :type :string :in :path}]}}
            (:paths (swaggerize routes)))))))

(deftest test-parameter-at-start
  (testing "parameter at start, without slash"
    (let [routes #{[:get "*"]}]
      (is (= {"/{*}" {:get {} :parameters [{:name :* :type :string :in :path}]}}
             (:paths (swaggerize routes)))))))

(deftest test-base
  (testing "deep merge of base specification"
    (let [routes #{[:get "/basic"]}]
      (is (= {:paths {"/basic" {:get {} :parameters ()}}
              :info {:title "My test title"
                     :version "0.1-SNAPSHOT"}
              :swagger "2.0"}
             (swaggerize routes :base {:info {:title "My test title"}}))))))

(deftest test-route-info
  (testing "route info specification"
    (let [routes #{[:get "/basic" {:swag {:description "My basic route"}}]}]
      (is (= {"/basic" {:get {:description "My basic route"}
                        :parameters ()}}
             (:paths (swaggerize routes :route-info :swag)))))))

(deftest test-expand-any
  (testing "expanding :any routes into all verbs"
    (let [routes #{[:any "/basic"]}]
      (is (= {"/basic" {:get {} :put {} :post {} :delete {} :options {} :head {} :patch {}
                        :parameters ()}}
             (:paths (swaggerize routes :route-info :swag)))))))

(deftest test-json
  (testing "writing json spec"
    (let [expected-start "{\"swagger\":\"2.0\""]
      (is (= expected-start (subs (swaggerize-json nil) 0 (count expected-start)))))))

(deftest test-yaml
  (testing "writing yaml spec"
    (let [expected-start "swagger: '2.0'"]
      (is (= expected-start (subs (swaggerize-yaml nil) 0 (count expected-start)))))))
