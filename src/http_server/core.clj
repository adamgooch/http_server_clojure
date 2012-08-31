(ns http-server.core
  (:require (http-server [server :refer (create-server)]
                         [headers :refer (get-headers)]
                         [get-request :refer (handle-get-request)]
                         [post-request :refer (handle-post-request)]
                         [util :refer (send-response log-request)])
            (clojure [string :only (split) :refer (split)]))
  (:import (java.io BufferedReader InputStreamReader)
           (java.net ServerSocket)
           (java.lang Integer))
  (:gen-class))

(def default-port 5000)
(def root-directory (ref ""))
(def no-host-message "No Host: header recevied")
(def put-content "Temporary PUT Request Content")

(defn- serve-client [out-stream request]
  (cond
    (= (first request) "GET")
      (handle-get-request out-stream @root-directory (second request))
    (= (first request) "PUT")
      (send-response out-stream (get-headers :ok :txt put-content) put-content)
    (= (first request) "POST")
      (handle-post-request out-stream @root-directory (second request))))

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
      (send-response out-stream
                     (get-headers :bad-request :txt no-host-message)
                     no-host-message)
      (serve-client out-stream (split (first request) #" "))))
  (.close in-stream)
  (.close out-stream))

(defn -main  [& args]
  (let [mapped-args (apply hash-map args)]
    (if (contains? mapped-args "-d")
      (dosync (ref-set root-directory (mapped-args "-d")))
      (dosync (ref-set root-directory "")))
    (if (contains? mapped-args "-p")
      (create-server handle-client (ServerSocket. (Integer/valueOf (mapped-args "-p"))))
      (create-server handle-client (ServerSocket. default-port)))))
