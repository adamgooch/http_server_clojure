(ns http-server.get-request
  (:require (http-server [headers :refer :all])
            (clojure [string :as string]))
  (:import (java.io PrintWriter InputStreamReader FileInputStream BufferedReader FileReader File)))

(def not-found-content "HTTP/1.1 404 File Not Found")

(defn- convert-spaces [file-name]
  (string/replace file-name "%20" " "))

(defn- file-not-found [out-stream]
  (send-headers out-stream {:status (get-status-header :not-found)
                            :type (get-type-header "txt")
                            :content-length (get-content-length-header not-found-content)
                            :body not-found-content}))

(defn get-file-type [file-name]
  (last (string/split file-name #"\.")))

(defn- send-file [out-stream file-path]
  (with-open [input-stream (FileInputStream. file-path)]
    (clojure.java.io/copy input-stream out-stream)))

(defn- get-dir-contents [path]
  (->> (File. path)
       .list
       (map #(str % "\n"))
       (apply str)))

(defn- serve-file [out-stream file-path]
  (if (.isDirectory (File. file-path))
    (let [dir-contents (get-dir-contents file-path)]
      (send-headers out-stream {:status (get-status-header :ok)
                                :type (get-type-header "txt")
                                :content-length (get-content-length-header dir-contents)
                                :body dir-contents}))
    (do
      (send-headers out-stream {:status (get-status-header :ok)
                                :type (get-type-header (get-file-type file-path))
                                :content-length (str content-length-prefix (.length (File. file-path)))})
      (send-file out-stream file-path))))

(defn- process-file [out-stream file-path]
  (if (.exists (File. file-path))
    (serve-file out-stream file-path)
    (file-not-found out-stream)))

(defn- server-root-request [out-stream]
  (let [body "Hello World"]
    (send-headers out-stream {:status (get-status-header :ok)
                              :type (get-type-header "txt")
                              :content-length (get-content-length-header body)
                              :body body})))

(defn- get-query-as-string [query]
  (->> (seq query)
       (map #(str (first %) " = " (last %) "\n"))
       (apply str)))

(defn- parse-query-string [query]
  (->> (string/split query #"&")
       (map #(string/split % #"="))
       (into {})))

(defn- echo-back-query-string [out-stream request]
  (let [query (parse-query-string (second (string/split request #"\?")))
        body (get-query-as-string query)]
    (send-headers out-stream {:status (get-status-header :ok)
                              :type (get-type-header "txt")
                              :content-length (get-content-length-header body)
                              :body body})))

(defn handle-get-request [out-stream root-directory request]
  (if (re-find #"\?" request)
    (echo-back-query-string out-stream request)
    (if (= request "/")
      (server-root-request out-stream)
      (let [file-path (convert-spaces (str root-directory request))]
        (process-file out-stream file-path)))))
