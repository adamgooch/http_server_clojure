(ns http-server.headers)

(def log-file-2 "/Users/Tank/temp/server_request_log.txt")

(defn get-status-header [status]
  (cond (= status :ok) '"HTTP/1.1 200 OK"
        (= status :not-found) '"HTTP/1.1 404 Not Found"
        (= status :bad-request) '"HTTP/1.1 400 Bad Request"))

(defn get-date-header []
  (str "Date: " (.toString (java.util.Date.))))

(defn get-content-length-header [file-name]
  (str "Content-Length: " (.length (java.io.File. file-name))))

(defn log-headers-sent [headers]
  (with-open [writer (clojure.java.io/writer log-file-2 :append true)]
    (doseq [line headers]
      (.write writer (str line "\n")))))


(defn send-headers [out-stream headers]
;  (log-headers-sent headers)
  (let [output (java.io.PrintWriter. out-stream)]
    (.println output (:status headers))
    (.println output (:date headers))
    (.println output (:type headers))
    (.println output (:content-length headers))
    (.println output (:blank-line headers))
    (when (contains? headers :body)
      (.println output (:body headers)))
    (.flush output)))

(defn get-type-header [file-type]
  (cond (= file-type "html") '"Content-Type: text/html"
        (= file-type "css") '"Content-Type: text/css"
        (= file-type "jpg") '"Content-Type: image/jpg"
        (= file-type "gif") '"Content-Type: image/gif"
        (= file-type "png") '"Content-Type: image/png"
        :else '"Content-Type: text/plain"))


