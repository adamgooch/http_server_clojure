(ns http-server.core-test
  (:require [clojure.test :refer :all]
            [http-server.core :refer :all]
            [http-server.get-request :refer :all])
  (:import (java.io BufferedReader InputStreamReader PrintWriter)))

(def header_1 "Host: 127.0.0.1:5000\n")
(def header_2 "User-Agent: HTTPTool/1.1\n\n")

(defn connect [port]
  (java.net.Socket. '"127.0.0.1" port))

(defn get-reader [socket]
  (BufferedReader. (InputStreamReader. (.getInputStream socket))))

(defn get-writer [socket]
  (PrintWriter. (.getOutputStream socket)))

(defn send-request [socket message]
  (let [out (get-writer socket) in (get-reader socket)]
  (. out println message)
  (. out flush)
  {:status-header (.readLine in)
   :date-header (.readLine in)
   :type-header (.readLine in)
   :content-length-header (.readLine in)
   :blank (.readLine in)
   :message (line-seq in)}))

(deftest http-server
  (def server (-main))

  (testing "should serve Hello World response to a request of / root")
    (is (= '("Hello World")
           (:message (send-request (connect default-port)
                     (str "GET / HTTP/1.1\n" header_1 header_2)))))

  (testing "should be able to serve a file from a given directory")
    (is (= '("This is a short test file." "It has three lines." "This is the last one.")
           (:message (send-request (connect default-port)
                     (str "GET /Users/Tank/Clojure/http-server/test/testFile HTTP/1.1\n" header_1 header_2)))))
  (testing "should respond with an appropriate error code if a given file does not exist")
    (is (= "HTTP/1.1 404 Not Found"
           (:status-header (send-request (connect default-port)
                           (str "GET /Unkown/File HTTP/1.1\n" header_1 header_2)))))
  (testing "should retun a Bad Request response if the request does not contain the host header")
    (is (= "HTTP/1.1 400 Bad Request"
           (:status-header (send-request (connect default-port)
                           (str "GET /Users/Tank/testFile HTTP/1.1\n" header_2)))))
  (testing "should have the correct type header for the requested file")
    (is (= "Content-Type: text/html"
           (:type-header (send-request (connect default-port)
                         (str "GET /Users/Tank/Sites/index.html HTTP/1.1\n" header_1 header_2)))))
  (testing "should translate %20 into spaces in the file path")
    (is (= "HTTP/1.1 200 OK"
           (:status-header (send-request (connect default-port)
              (str "GET /Users/Tank/Clojure/http-server/test/test%20file%202 HTTP/1.1\n" header_1 header_2)))))

  (testing "should receive directory contents when given GET directory")
    (is (= '(".DS_Store" "http_server" "test file 2" "testFile" "")
           (:message (send-request (connect default-port)
                     (str "GET /Users/Tank/Clojure/http-server/test HTTP/1.1\n" header_1 header_2)))))

  (. (get server :server-socket) close))

(deftest get-file-contents-when-server-is-pointed-at-a-directory
  (def server (-main "-d" "/Users/Tank/Clojure/http-server"))

  (testing "should be able to point it to a directory")
    (is (= '("This is a short test file." "It has three lines." "This is the last one.")
           (:message (send-request (connect default-port)
                     (str "GET /test/testFile HTTP/1.1\n" header_1 header_2)))))

  (. (get server :server-socket) close))

(deftest get-directory-contents-when-server-is-pointed-at-a-directory
  (def server (-main "-d" "/Users/Tank/Clojure/http-server"))

  (testing "should receive directory contents when given GET /test")
    (is (= '(".DS_Store" "http_server" "test file 2" "testFile" "")
            (:message (send-request (connect default-port)
                      (str "GET /test HTTP/1.1\n" header_1 header_2)))))

  (. (get server :server-socket) close))

(deftest get-file-type-test
  (testing "should return html when given this file type")
    (is (= "html" (get-file-type "Users/Tank/Sites/index.html"))))

(deftest type-header-test
  (testing "should return the correct content type header")
    (is (= "Content-Type: text/html" (http-server.headers/get-type-header "html"))))

(deftest get-with-query
  (def server (-main "-d" "/Users/Tank/Clojure/http-server"))

  (testing "should echo back the query string")
    (is (= '("variable1 = you" "variable2 = me")
           (:message (send-request (connect default-port)
                                (str "GET /test?variable1=you&variable2=me HTTP/1.1\n" header_1 header_2)))))
  (. (get server :server-socket) close))

(deftest start-server-with-specified-port
  (def server (-main "-p" "1024"))

  (testing "should accept connection on port 1024")
    (is (= "HTTP/1.1 200 OK"
           (:status-header (send-request (connect 1024)
              (str "GET /Users/Tank/ HTTP/1.1\n" header_1 header_2)))))

  (. (get server :server-socket) close))

;  (testing "should handle multiple simultaneous requests")
