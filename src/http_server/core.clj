(ns http-server.core
  (:use [http-server.server])
  (:import (java.io BufferedReader InputStreamReader PrintWriter FileReader DataInputStream FileInputStream)))

(def port 1024)
(def root-directory (ref ""))

(defn get-status-header [status]
  (cond (= status :ok) '"HTTP/1.1 200 OK"
        (= status :not-found) '"HTTP/1.1 404 Not Found"
        (= status :bad-request) '"HTTP/1.1 400 Bad Request"))

(defn get-date-header []
  (str "Date: " (.toString (java.util.Date.))))

(defn get-type-header [file-type]
  (cond (= file-type "html") '"Content-Type: text/html"
        (= file-type "css") '"Content-Type: text/css"
        (= file-type "jpg") '"Content-Type: image/jpg"
        (= file-type "gif") '"Content-Type: image/gif"
        (= file-type "png") '"Content-Type: image/png"
        :else '"Content-Type: text/plain"))

(defn convert-spaces [file-name]
  (clojure.string/replace file-name "%20" " "))

(defn get-content-length-header [file-name]
  (.length (java.io.File. (str @root-directory (convert-spaces file-name)))))

(defn send-headers [out-stream headers]
  (let [output (PrintWriter. out-stream)]
    (.println output (:status headers))
    (.println output (:date headers))
    (.println output (:type headers))
    (.println output (str "Content-Length: " (:content-length headers)))
    (.println output (:blank-line headers))
    (.flush output)))

(defn send-body [out-stream message]
  (let [output (PrintWriter. out-stream)]
    (.println output message)
    (.close output)))

(defn file-not-found [out-stream headers]
  (send-headers out-stream (assoc headers :status (get-status-header :not-found)
                                          :content-length (.length "HTTP/1.1 404 Not Found")))
  (send-body out-stream "HTTP/1.1 404 Not Found"))


(defn get-file-type [file-name]
  (last (clojure.string/split file-name #"\.")))

(defn is-media [type-header]
  (= "image" (subs type-header 14 19)))

(defn send-media-file [input-stream out-stream]
  (with-open [out (clojure.java.io/output-stream out-stream)]
    (loop [c (.read input-stream)]
      (if (not= c -1)
        (do
          (.write out c)
          (recur (.read input-stream)))))))

(defn send-media [out-stream file-name headers]
    (try
      (with-open [input (clojure.java.io/input-stream (str @root-directory (convert-spaces file-name)))]
        (send-headers out-stream (assoc headers :status (get-status-header :ok)
                                                :content-length (get-content-length-header file-name)))
        (send-media-file input out-stream))
    (catch java.io.FileNotFoundException exception
      (file-not-found out-stream headers))))

(defn send-text-file [reader out-stream]
  (with-open [out (PrintWriter. out-stream)]
    (loop [line (.readLine reader)]
      (if (not= line nil)
        (do
          (.println out line)
          (recur (.readLine reader)))))))

(defn send-text [out-stream file-name headers]
  (try
    (with-open [reader (BufferedReader.
                       (InputStreamReader.
                       (DataInputStream.
                       (FileInputStream. (str @root-directory (convert-spaces file-name))))))]
      (send-headers out-stream (assoc headers :status (get-status-header :ok)
                                              :content-length (get-content-length-header file-name)))
      (send-text-file reader out-stream))
  (catch java.io.FileNotFoundException exception
    (file-not-found out-stream headers))))

(defn process-file [out-stream file-name headers]
  (if (is-media (headers :type))
    (send-media out-stream file-name headers)
    (send-text out-stream file-name headers)))

(defn has-valid-host-header [host-line]
  (= (first (clojure.string/split host-line #" ")) "Host:"))

(defn serve-client [request headers out-stream]
  (let [file-name (second request)]
    (when (= (first request) "GET")
      (if (= file-name "/")
        (do
          (send-headers out-stream (assoc headers :status (get-status-header :ok)
                                                  :content-length (.length "Hello World")))
          (send-body out-stream "Hello World"))
        (process-file out-stream
                      file-name
                      (assoc headers :type (get-type-header (get-file-type file-name))))))))

(defn handle-client [in-stream out-stream]
  (with-open [input (BufferedReader. (InputStreamReader. in-stream))]
    (let [request (clojure.string/split (.readLine input) #" ")
          headers {:date (get-date-header), :type (get-type-header "txt"), :blank-line ""}]
      (if (has-valid-host-header (.readLine input))
        (serve-client request headers out-stream)
        (do
          (send-headers out-stream (assoc headers :status (get-status-header :bad-request)))
          (send-body out-stream "No Host: header recevied"))))))

(defn start-server []
  (create-server handle-client (java.net.ServerSocket. port)))

(defn -main
  "Start the server."
  [& args]
  (dosync (ref-set root-directory (first args)))
  (println "server started...")
  (start-server)
)
