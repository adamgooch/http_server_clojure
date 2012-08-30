(ns http-server.headers
  (:import (java.io File PrintWriter)
           (java.util Date)))

(def log-file-2 "/Users/Tank/temp/server_request_log.txt")
(def content-length-prefix "Content-Length: ")

(defn get-status-header [status]
  (cond (= status :ok) "HTTP/1.1 200 OK"
        (= status :not-found) "HTTP/1.1 404 Not Found"
        (= status :bad-request) "HTTP/1.1 400 Bad Request"))

(defn get-date-header []
  (str "Date: " (.toString (Date.))))

(defn get-content-length-header [content]
  (str content-length-prefix (.length content)))

(defn get-type-header [file-type]
  (cond (= file-type "html") "Content-Type: text/html"
        (= file-type "css") "Content-Type: text/css"
        (= file-type "jpg") "Content-Type: image/jpg"
        (= file-type "gif") "Content-Type: image/gif"
        (= file-type "png") "Content-Type: image/png"
        :else "Content-Type: text/plain"))

(defn- log-headers-sent [headers]
  (with-open [writer (clojure.java.io/writer log-file-2 :append true)]
    (doseq [line headers]
      (.write writer (str line "\n")))))

(defn send-headers [out-stream headers]
;  (log-headers-sent headers)
  (let [output (PrintWriter. out-stream)]
    (.println output (:status headers))
    (.println output (get-date-header))
    (.println output (:type headers))
    (.println output (:content-length headers))
    (.println output "")
    (when (contains? headers :body)
      (.println output (:body headers)))
    (.flush output)))
