(ns http-server.get-request
  (:require (http-server [headers :refer :all]))
  (:import (java.io PrintWriter InputStreamReader FileInputStream BufferedReader FileReader File)))

(def root-directory (ref ""))
(def not-found-content "HTTP/1.1 404 Not Found")

(defn- convert-spaces [file-name]
  (clojure.string/replace file-name "%20" " "))

(defn- file-not-found [out-stream headers]
  (send-headers out-stream (assoc headers :status (get-status-header :not-found)
                                :content-length (str "Content Length: " (.length not-found-content))
                                :body not-found-content)))

(defn get-file-type [file-name]
  (last (clojure.string/split file-name #"\.")))

(defn- send-file [out-stream file-path]
  (with-open [input-stream (FileInputStream. file-path)]
    (clojure.java.io/copy input-stream out-stream)))

(defn serve-file [out-stream file-path headers]
  (if (.isDirectory (File. file-path))
    (let [dir-contents (apply str (map #(str % "\n") (.list (File. file-path))))]
      (send-headers out-stream (assoc headers :status (get-status-header :ok)
                               :content-length (str "Content-Length: " (.length dir-contents))
                               :body dir-contents)))
    (do
      (send-headers out-stream (assoc headers :status (get-status-header :ok)
                                      :content-length (get-content-length-header file-path)))
      (send-file out-stream file-path))))

(defn- process-file [out-stream file-path headers]
  (if (.exists (File. file-path))
    (serve-file out-stream file-path headers)
    (file-not-found out-stream headers)))

(defn- echo-back-query-string [out-stream request headers]
  (let [request (clojure.string/split request #"\?")
        body (clojure.string/replace (clojure.string/replace (second request) "=" " = ") "&" "\n")]
    (send-headers out-stream (assoc headers :status (get-status-header :ok)
                                    :content-length (str "Content-Length: " (+ 1 (.length body)))
                                    :body body))))

(defn handle-get-request [out-stream request headers]
  (if (re-find #"\?" request)
    (echo-back-query-string out-stream request headers)
    (if (= request "/")
      (send-headers out-stream (assoc headers :status (get-status-header :ok)
                                        :content-length (str "Content-Length: " (.length "Hello World"))
                                        :body "Hello World"))
      (let [file-path (convert-spaces (str @root-directory request))]
        (process-file out-stream file-path
                                 (assoc headers :type (get-type-header (get-file-type request))))))))
