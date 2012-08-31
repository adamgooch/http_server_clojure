(ns http-server.core-test
  (:require [clojure.test :refer :all]
            [http-server.core :refer :all])
  (:import (java.io BufferedReader InputStreamReader PrintWriter)))

(def host-header "Host: 127.0.0.1:5000\n")
(def user-agent-header "User-Agent: HTTPTool/1.1\n\n")

(defn connect [port]
  (java.net.Socket. "localhost" port))

(defn send-request [socket request]
  (let [out (PrintWriter. (.getOutputStream socket))]
    (.println out request)
    (.println out host-header)
    (.println out user-agent-header)
    (.println out "")
    (.flush out)))

(defn send-request-without-host-header [socket request]
  (let [out (PrintWriter. (.getOutputStream socket))]
    (.println out request)
    (.println out user-agent-header)
    (.println out "")
    (.flush out)))

(defn get-response [socket]
  (let [in (BufferedReader. (InputStreamReader. (.getInputStream socket)))]
    {:status-header (.readLine in)
     :date-header (.readLine in)
     :type-header (.readLine in)
     :content-length-header (.readLine in)
     :blank (.readLine in)
     :message (line-seq in)}))

(deftest get-request-of-slash
  (testing "should serve Hello World response")
    (let [server (-main)
          socket (connect default-port)]
      (send-request socket "GET / HTTP/1.1")
      (is (= '("Hello World")
              (:message (get-response socket))))
      (.close server)
      (.close socket)))

(deftest serve-file-contents
  (testing "should be able to serve a file from a given directory")
    (let [server (-main)
          socket (connect default-port)]
      (send-request socket "GET /Users/Tank/Clojure/http-server/test/testFile HTTP/1.1")
      (is (= '("This is a short test file." "It has three lines." "This is the last one.")
              (:message (get-response socket))))
      (.close server)
      (.close socket)))

(deftest unknown-file
  (testing "should respond with a 404 response if a given file does not exist")
    (let [server (-main)
          socket (connect default-port)]
      (send-request socket "GET /Unkown/File HTTP/1.1")
      (is (= "HTTP/1.1 404 Not Found"
             (:status-header (get-response socket))))
      (.close server)
      (.close socket)))

(deftest no-host-header
  (testing "should retun a Bad Request response if the request does not contain the host header")
    (let [server (-main)
          socket (connect default-port)]
      (send-request-without-host-header socket "GET /Users/Tank/testFile HTTP/1.1")
      (is (= "HTTP/1.1 400 Bad Request"
             (:status-header (get-response socket))))
      (.close server)
      (.close socket)))

(deftest type-header
  (testing "should have the correct type header for the requested file")
    (let [server (-main)
          socket (connect default-port)]
      (send-request socket "GET /Users/Tank/Sites/index.html HTTP/1.1")
      (is (= "Content-Type: text/html"
             (:type-header (get-response socket))))
      (.close server)
      (.close socket)))

(deftest convert-browser-spaces-into-actual-spaces
  (testing "should translate %20 into spaces in the file path")
    (let [server (-main)
          socket (connect default-port)]
      (send-request socket "GET /Users/Tank/Clojure/http-server/test/test%20file%202 HTTP/1.1")
      (is (= "HTTP/1.1 200 OK"
             (:status-header (get-response socket))))
      (.close server)
      (.close socket)))

(deftest directory-contents
  (testing "should receive directory contents when given GET directory")
    (let [server (-main)
          socket (connect default-port)]
      (send-request socket "GET /Users/Tank/Clojure/http-server/test HTTP/1.1")
      (is (not (nil? (re-find #".DS_Store.*http_server.*test file 2.*testFile"
              (first (:message (get-response socket)))))))
      (.close server)
      (.close socket)))

(deftest handle-get-query-with-no-value
  (testing "should return the variable with a value of blank")
    (let [server (-main)
          socket (connect default-port)]
      (send-request socket "GET /test?variable1&variable2 HTTP/1.1")
    (is (= '("variable1 = " "variable2 = ")
            (:message (get-response socket))))
      (.close server)
      (.close socket)))

(deftest get-file-contents-when-server-is-pointed-at-a-directory
  (testing "should be able to point it to a directory")
    (let [server (-main "-d" "/Users/Tank/Clojure/http-server")
          socket (connect default-port)]
      (send-request socket "GET /test/testFile HTTP/1.1")
      (is (= '("This is a short test file." "It has three lines." "This is the last one.")
              (:message (get-response socket))))
      (.close server)
      (.close socket)))

(deftest get-directory-contents-when-server-is-pointed-at-a-directory
  (testing "should receive directory contents when given GET /test")
    (let [server (-main "-d" "/Users/Tank/Clojure/http-server")
          socket (connect default-port)]
      (send-request socket "GET /test HTTP/1.1")
      (is (not (nil? (re-find #".DS_Store.*http_server.*test file 2.*testFile"
              (first (:message (get-response socket)))))))
      (.close server)
      (.close socket)))

(deftest get-containing-query
  (testing "should echo back the query string")
    (let [server (-main "-d" "/Users/Tank/Clojure/http-server")
          socket (connect default-port)]
      (send-request socket "GET /test?variable1=you&variable2=me HTTP/1.1")
      (is (= '("variable1 = you" "variable2 = me")
              (:message (get-response socket))))
      (.close server)
      (.close socket)))

(deftest start-server-with-specified-port
  (testing "should accept connection on port 1024")
    (let [server (-main "-p" "1024")
          socket (connect 1024)]
      (send-request socket "GET /Users/Tank/ HTTP/1.1")
      (is (= "HTTP/1.1 200 OK"
             (:status-header (get-response socket))))
      (.close server)
      (.close socket)))

(deftest process-script-with-post
  (testing "should return Hello World web page with a variable name embedded")
    (let [server (-main "-d" "/Users/Tank/Clojure/http-server/cob_spec")
          socket (connect default-port)]
      (send-request socket "POST /public/simple_cgi.rb HTTP/1.1")
      (is (= '("<html><body><h1>Hello Adam!</h1></body></html>")
              (:message (get-response socket))))
      (.close server)
      (.close socket)))

