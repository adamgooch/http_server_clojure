(ns http-server.core-test
  (:use clojure.test
        http-server.core)
  (:import (java.io BufferedReader InputStreamReader PrintWriter)))

(def header_1 "Host: 127.0.0.1:1024")
(def header_2 "User-Agent: HTTPTool/1.1\n\n")

(defn connect []
  (new java.net.Socket '"127.0.0.1" '1024))

(defn get-reader [socket]
  (new BufferedReader (new InputStreamReader (. socket getInputStream))))

(defn get-writer [socket]
  (new PrintWriter (. socket getOutputStream)))

(defn send-request [socket, message]
  (def out (get-writer socket))
  (def in (get-reader socket))
  (. out println message)
  (. out flush)
  {:header1 (. in readLine)
   :header2 (. in readLine)
   :header3 (. in readLine)
   :blank (. in readLine)
   :message (. in readLine)}
)

(deftest http-server
  (-main)
  (testing "should accept a connection on port 1024")
    (is (= "Hello World"
           (:message (send-request (connect) (str "GET / HTTP/1.1\n" header_1 header_2)))))
  (testing "should serve Hello World response to a request of / root")
    (is (= "Hello World"
           (:message (send-request (connect) "GET / HTTP/1.1"))))
)
;  (testing "should be able to point it to a directory")
;  (testing "should be able to serve file from a given directory")
;  (testing "should respond with an appropriate error code if a given file does not exist")
;  (testing "should handle multiple simultaneous requests")
