(ns http-server.core
  (:require (http-server [server :only (create-server) :refer (create-server)]
                         [headers :refer :all]
                         [get-request :refer (handle-get-request)]
                         [post-request :refer (handle-post-request)])
            (clojure [string :only (split) :refer (split)]))
  (:import (java.io BufferedReader InputStreamReader)
           (java.net ServerSocket)
           (java.lang Integer))
  (:gen-class))

(def default-port 5000)
(def root-directory (ref ""))
(def log-file "/Users/Tank/temp/server_request_headers_log.txt")
(def no-host-message "No Host: header recevied")
(def put-content "Temporary PUT Request Content")

(defn handle-put-request [out-stream request]
  (send-headers out-stream {:status (get-status-header :ok)
                            :type (get-type-header "txt")
                            :content-length (str content-length-prefix (.length put-content))
                            :body put-content}))

(defn serve-client [out-stream request]
  (cond
    (= (first request) "GET")
      (handle-get-request out-stream @root-directory (second request))
    (= (first request) "PUT")
      (handle-put-request out-stream (second request))
    (= (first request) "POST")
      (handle-post-request out-stream @root-directory (second request))))

(defn send-no-host-reply [out-stream]
  (send-headers out-stream {:status (get-status-header :bad-request)
                            :type (get-type-header "txt")
                            :content-length (get-content-length-header no-host-message)
                            :body no-host-message}))

(defn- log-request [request]
  (with-open [writer (clojure.java.io/writer log-file :append true)]
    (.write writer (str "root directory: " @root-directory "\n\n"))
    (doseq [line request]
      (.write writer (str line "\n")))))

(defn- acquire-request-headers [in-stream]
  "Gets all input from the input stream until the first blank line."
  (let [input (BufferedReader. (InputStreamReader. in-stream))]
    (loop [request (vector (.readLine input))]
      (if (empty? (last request))
          request
          (recur (conj request (.readLine input)))))))

(defn- handle-client [in-stream out-stream]
  (let [request (acquire-request-headers in-stream)]
;    (log-request request)
    (if (empty? (filter #(re-find #"Host: " %) request))
      (send-no-host-reply out-stream)
      (serve-client out-stream (split (first request) #" "))))
  (.close in-stream))

(defn -main  [& args]
  (let [mapped-args (apply hash-map args)]
    (if (contains? mapped-args "-d")
      (dosync (ref-set root-directory (mapped-args "-d")))
      (dosync (ref-set root-directory "")))
    (if (contains? mapped-args "-p")
      (create-server handle-client (ServerSocket. (Integer/valueOf (mapped-args "-p"))))
      (create-server handle-client (ServerSocket. default-port)))))
