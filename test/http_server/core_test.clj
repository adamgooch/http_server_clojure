(ns http-server.core-test
  (:use clojure.test
        http-server.core)
  (:import (java.io BufferedReader InputStreamReader PrintWriter)))

(def header_1 "Host: 127.0.0.1:1024\n")
(def header_2 "User-Agent: HTTPTool/1.1\n\n")

(defn connect []
  (java.net.Socket. '"127.0.0.1" '1024))

(defn get-reader [socket]
  (BufferedReader. (InputStreamReader. (. socket getInputStream))))

(defn get-writer [socket]
  (PrintWriter. (. socket getOutputStream)))

(defn send-request [socket message]
  (let [out (get-writer socket) in (get-reader socket)]
  (. out println message)
  (. out flush)
  {:header1 (. in readLine)
   :header2 (. in readLine)
   :header3 (. in readLine)
   :blank (. in readLine)
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
           (:header1 (send-request (connect)
                                   (str "GET /Unkown/File HTTP/1.1\n" header_1 header_2)))))
  (testing "should retun a Bad Request response if the request does not contain the host header")
    (is (= "HTTP/1.1 400 Bad Request"
           (:header1 (send-request (connect)
                                   (str "GET /Users/Tank/testFile HTTP/1.1\n" header_2)))))

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
