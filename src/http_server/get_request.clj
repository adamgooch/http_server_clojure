(ns http-server.get-request
  (:require (http-server [headers :refer (get-headers)]
                         [util :refer (send-response)])
            (clojure [string :as string]))
  (:import (java.io PrintWriter InputStreamReader FileInputStream BufferedReader FileReader File)))

(def not-found-content "HTTP/1.1 404 File Not Found")
(def root-response "Hello World")
(def default-file-type "txt")

(defn- convert-spaces [file-name]
  (string/replace file-name "%20" " "))

(defn- get-file-type [file-name]
  (if (not (nil? (re-find #"\." file-name)))
    (last (string/split file-name #"\."))
    default-file-type))

(defn- send-file [out-stream file-path]
  (with-open [input-stream (FileInputStream. file-path)]
    (clojure.java.io/copy input-stream out-stream)))

(defn- get-dir-contents [path]
  (->> (File. path)
       .list
       (map #(str "<a href= '" path "/" % "'>" % "</a><br/>"))
       (apply str)))

(defn- serve-file [out-stream file-path]
  (if (.isDirectory (File. file-path))
    (let [dir-contents (get-dir-contents file-path)]
      (send-response out-stream (get-headers :ok :html dir-contents) dir-contents))
    (do
      (send-response out-stream (get-headers :ok (keyword (get-file-type file-path)) (File. file-path)) nil)
      (send-file out-stream file-path))))

(defn- process-file [out-stream encoded-file-path]
  (let [file-path (convert-spaces encoded-file-path)]
    (if (.exists (File. file-path))
      (serve-file out-stream file-path)
      (send-response out-stream (get-headers :not-found :txt not-found-content) not-found-content))))

(defn- get-query-as-string [query]
  (->> (map #(str (first %) " = " (last %) "\n") query)
       (apply str)))

(defn- get-key-value-pairs [query-parameter]
  (if (re-find #"=" query-parameter)
    (string/split query-parameter #"=")
    [query-parameter ""]))

(defn- parse-query-string [query]
  (map get-key-value-pairs (string/split query #"&")))

(defn- echo-back-query-string [out-stream request]
  (let [query (parse-query-string (second (string/split request #"\?")))
        body (get-query-as-string query)]
    (send-response out-stream (get-headers :ok :txt body) body)))

(defn handle-get-request [out-stream root-directory request]
  (if (re-find #"\?" request)
    (echo-back-query-string out-stream request)
    (if (= request "/")
      (send-response out-stream (get-headers :ok :txt root-response) root-response)
      (process-file out-stream (str root-directory request)))))
