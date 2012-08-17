(ns http-server.core-test
  (:use clojure.test
        http-server.core)
  (:import (java.io BufferedReader InputStreamReader PrintWriter)))

(def header_1 "Host: 127.0.0.1:1024\n")
(def header_2 "User-Agent: HTTPTool/1.1\n\n")

(defn connect []
  (java.net.Socket. '"127.0.0.1" '1024))

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
   :blank (.readLine in)
   :message (line-seq in)}))

(deftest http-server
  (def server (-main))

  (testing "should serve Hello World response to a request of / root")
    (is (= '("Hello World")
           (:message (send-request (connect)
                                   (str "GET / HTTP/1.1\n" header_1 header_2)))))
  (testing "should be able to serve a file from a given directory")
    (is (= '("This is a short test file." "It has three lines." "This is the last one.")
           (:message (send-request (connect)
                                   (str "GET /Users/Tank/testFile HTTP/1.1\n" header_1 header_2)))))
  (testing "should respond with an appropriate error code if a given file does not exist")
    (is (= "HTTP/1.1 404 Not Found"
           (:status-header (send-request (connect)
                                   (str "GET /Unkown/File HTTP/1.1\n" header_1 header_2)))))
  (testing "should retun a Bad Request response if the request does not contain the host header")
    (is (= "HTTP/1.1 400 Bad Request"
           (:status-header (send-request (connect)
                                   (str "GET /Users/Tank/testFile HTTP/1.1\n" header_2)))))
  (testing "should have the correct type header for the requested file")
    (is (= "Content-Type: text/html"
           (:type-header (send-request (connect)
                                  (str "GET /Users/Tank/Sites/index.html HTTP/1.1\n" header_1 header_2)))))
  (testing "should translate %20 into spaces in the file path")
    (is (= "HTTP/1.1 200 OK"
           (:status-header (send-request (connect)
                                   (str "GET /Users/Tank/test%20file%202 HTTP/1.1\n" header_1 header_2)))))

  (. (get server :server-socket) close)
)

(deftest http-server2
  (def server (-main "/Users/Tank"))

  (testing "should be able to point it to a directory")
    (is (= '("This is a short test file." "It has three lines." "This is the last one.")
           (:message (send-request (connect)
                                   (str "GET /testFile HTTP/1.1\n" header_1 header_2)))))

  (. (get server :server-socket) close)
)

;  (testing "should handle multiple simultaneous requests")
