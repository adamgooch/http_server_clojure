(ns http-server.core
  (:use ;[clojure.contrib.server-socket]
        [http-server.server])
  (:import (java.io BufferedReader InputStreamReader PrintWriter FileReader)))

(def port 1024)
(def root-directory (ref ""))

(defn status-header [status]
  (cond (= status :ok) '"HTTP/1.1 200 OK"
        (= status :not-found) '"HTTP/1.1 404 Not Found"
        (= status :bad-request) '"HTTP/1.1 400 Bad Request"))

(defn date-header []
  (str "Date: " (.toString (java.util.Date.))))

(defn type-header [file-type]
  (cond (= file-type "html") '"Content-Type: text/html"
        (= file-type "txt") '"Content-Type: text/plain"
        (= file-type "css") '"Content-Type: text/css"
        (= file-type "jpg") '"Content-Type: image/jpg"
        (= file-type "gif") '"Content-Type: image/gif"))

(defn send-response [output response]
  (.println output (:status response))
  (.println output (:date response))
  (.println output (:type response))
  (.println output (:blank-line response))
  (.println output (:body response)))

(defn get-file-type [file-name]
  (last (clojure.string/split file-name #"\.")))

(defn convert-spaces [file-name]
  (clojure.string/replace file-name "%20" " "))

(defn process-file [out file-name response]
  (try
    (with-open [reader (clojure.java.io/reader (str @root-directory (convert-spaces file-name)))]
      (.println out (status-header :ok))
      (.println out (date-header))
      (.println out (type-header (get-file-type file-name)))
      (.println out "")
      (doseq [line (line-seq reader)]
        (.println out line)))
  (catch java.io.FileNotFoundException exception
    (send-response out (assoc response :status (status-header :not-found)
                                       :type (type-header "txt")
                                       :body "HTTP/1.1 404 Not Found")))))

(defn has-valid-host-header [host-line]
  (= (first (clojure.string/split host-line #" ")) "Host:"))

(defn is-get-request [request]
  (= (first request) "GET"))

(defn handle-client [in out]
  (println "connected")
  (let [input (BufferedReader. (InputStreamReader. in))
        output (PrintWriter. out)
        request (clojure.string/split (.readLine input) #" ")
        response {:date (date-header), :blank-line ""}]
  (if (has-valid-host-header (.readLine input))
    (when (is-get-request request)
      (if (= (second request) "/")
        (send-response output (assoc response :status (status-header :ok)
                                              :type (type-header "txt")
                                              :body "Hello World"))
        (process-file output (second request) response)))
    (send-response output (assoc response :status (status-header :bad-request)
                                          :type (type-header "txt")
                                          :body "No Host: header recevied")))
  (println (second request))
  (. output close)))

(defn start-server []
  (create-my-server port handle-client))

(defn -main
  "Start the server."
  [& args]
  (dosync (ref-set root-directory (first args)))
  (println "server started...")
  (start-server)
)
