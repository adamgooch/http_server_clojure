(ns http-server.core-test
  (:use clojure.test
        http-server.core))

(deftest http-server
  (testing "should receive a connection on port 80")
    (is (= 0 1)))
;  (testing "should serve Hello World response to a request of / root")
;  (testing "should be able to point it to a directory")
;  (testing "should be able to serve file from a given directory")
;  (testing "should respond with an appropriate error code if a given file does not exist")
;  (testing "should handle multiple simultaneous requests")
