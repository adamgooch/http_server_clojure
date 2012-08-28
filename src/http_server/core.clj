(ns http-server.core
  (:require (http-server [server :refer :all]
                         [headers :refer :all]
                         [get-request :refer :all])
            (clojure [string :only (split) :as string]))
  (:import (java.io BufferedReader InputStreamReader))
  (:gen-class))

(def default-port 5000)
(def log-file "/Users/Tank/temp/server_request_log.txt")
(def no-host-message "No Host: header recevied")
(def content-length-prefix "Content-Length: ")
(def post-content "Temporary POST Request Content")
(def put-content "Temporary PUT Request Content")

(defn handle-post-request [out-stream request headers]
  (send-headers out-stream (assoc headers :status (get-status-header :ok)
                                  :content-length (str content-length-prefix (.length post-content))
                                  :body post-content)))

(defn handle-put-request [out-stream request headers]
  (send-headers out-stream (assoc headers :status (get-status-header :ok)
                                  :content-length (str content-length-prefix (.length put-content))
                                  :body put-content)))

(defn serve-client [out-stream request headers]
    (cond
      (= (first request) "GET")
        (handle-get-request out-stream (second request) headers)
      (= (first request) "PUT")
        (handle-put-request out-stream (second request) headers)
      (= (first request) "POST")
        (handle-post-request out-stream (second request) headers)))

(defn send-no-host-header [out-stream headers]
  (send-headers out-stream (assoc headers :status (get-status-header :bad-request)
                                :content-length (str content-length-prefix (.length no-host-message))
                                :body no-host-message)))

(defn log-request [request]
  (with-open [writer (clojure.java.io/writer log-file :append true)]
    (.write writer (str "root directory: " @root-directory "\n"))
    (doseq [line (reverse request)]
      (.write writer (str line "\n")))))

(defn acquire-request [in-stream]
  (let [input (BufferedReader. (InputStreamReader. in-stream))]
    (loop [request (list (.readLine input))]
      (if (empty? (first request))
          request
          (recur (conj request (.readLine input)))))))

(defn handle-client [in-stream out-stream]
  (let [request (acquire-request in-stream)
        headers {:date (get-date-header), :type (get-type-header "txt"), :blank-line ""}]
;      (log-request request)
      (if (empty? (filter #(re-find #"Host: " %) request))
        (send-no-host-header out-stream headers)
        (serve-client out-stream (string/split (last request) #" ") headers))))

(defn -main  [& args]
  "Start the server."
  (let [mapped-args (apply hash-map args)]
    (if (contains? mapped-args "-d")
      (dosync (ref-set root-directory (mapped-args "-d")))
      (dosync (ref-set root-directory "")))
    (if (contains? mapped-args "-p")
      (create-server handle-client (java.net.ServerSocket. (java.lang.Integer/valueOf (mapped-args "-p"))))
      (create-server handle-client (java.net.ServerSocket. default-port)))))
