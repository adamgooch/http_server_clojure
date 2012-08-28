(ns http-server.get-request
  (:require (http-server [headers :refer :all]))
  (:import (java.io PrintWriter InputStreamReader FileInputStream BufferedReader FileReader)))

(def root-directory (ref ""))

(defn- convert-spaces [file-name]
  (clojure.string/replace file-name "%20" " "))

(defn send-body [out-stream message]
  (let [output (PrintWriter. out-stream)]
    (.println output message)
    (.close output)))

(defn- file-not-found [out-stream headers]
  (send-headers out-stream (assoc headers :status (get-status-header :not-found)
                                          :content-length (.length "HTTP/1.1 404 Not Found")))
  (send-body out-stream "HTTP/1.1 404 Not Found"))

(defn get-file-type [file-name]
  (last (clojure.string/split file-name #"\.")))

(defn- send-file [out-stream file-path]
  (with-open [input-stream (FileInputStream. file-path)]
    (clojure.java.io/copy input-stream out-stream)))

(defn get-dir-contents [file]
  (loop [contents "" dir (.list (java.io.File. file))]
    (if (= (count dir) 0)
      (subs contents 1)
      (recur (str contents "\n" (first dir)) (rest dir)))))

(defn- process-file [out-stream file-path headers]
  (if (.exists (java.io.File. file-path))
    (if (.isDirectory (java.io.File. file-path))
      (do
        (send-headers out-stream (assoc headers :status (get-status-header :ok)
                              :content-length (str "Content-Length: " (.length (get-dir-contents file-path)))))
        (send-body out-stream (get-dir-contents file-path)))
      (do
        (send-headers out-stream (assoc headers :status (get-status-header :ok)
                                        :content-length (get-content-length-header file-path)))
        (send-file out-stream file-path)))
    (file-not-found out-stream headers)))

(defn- echo-back-query-string [out-stream request headers]
  (let [request (clojure.string/split request #"\?")
        body (clojure.string/replace (clojure.string/replace (second request) "=" " = ") "&" "\n")]
    (send-headers out-stream (assoc headers :status (get-status-header :ok)
                                    :content-length (str "Content-Length: " (+ 1 (.length body)))))
    (send-body out-stream body)))

(defn handle-get-request [out-stream request headers]
  (if (re-find #"\?" request)
    (echo-back-query-string out-stream request headers)
    (if (= request "/")
      (do
        (send-headers out-stream (assoc headers :status (get-status-header :ok)
                                           :content-length (str "Content-Length: " (.length "Hello World"))))
        (send-body out-stream "Hello World"))
      (let [file-path (convert-spaces (str @root-directory request))]
        (process-file out-stream file-path
                           (assoc headers :type (get-type-header (get-file-type request))))))))
