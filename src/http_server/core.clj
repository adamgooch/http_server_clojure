(ns http-server.core
  (:use ;[clojure.contrib.server-socket]
        [http-server.server])
  (:import (java.io BufferedReader InputStreamReader PrintWriter FileReader)))

(def port 1024)
(def root-directory (ref ""))

(defn status-header [status]
  (cond (= status :ok) (str "HTTP/1.1 200 OK\n")
        (= status :not-found) (str "HTTP/1.1 404 Not Found\n")
        (= status :bad-request) (str "HTTP/1.1 400 Bad Request\n")))

(defn date-header []
  (str "Date: " (. (java.util.Date.) toString) "\n"))

(defn type-header [file-type]
  (if (= file-type :html)
    (str "Content-Type: text/html\n")))

(defn process-file [out file-name]
  (try
    (with-open [reader (BufferedReader. (FileReader. (str @root-directory file-name)))]
      (. out (println (str (status-header :ok) (date-header) (type-header :html) )))
      (doseq [line (line-seq reader)] (. out (println line))))
  (catch java.io.FileNotFoundException exception
    (. out (println (str (status-header :not-found) (date-header) (type-header :html) "\nHTTP/1.1 404 Not Found"))))))

(defn has-valid-host-header [host-line]
  (= (first (clojure.string/split host-line #" ")) "Host:"))

(defn is-get-request [request]
  (= (first request) "GET"))

(defn handle-client [in out]
  (println "connected")
  (let [input (BufferedReader. (InputStreamReader. in))
        output (PrintWriter. out)
        request (clojure.string/split (. input readLine) #" ")]
  (if (has-valid-host-header (. input readLine))
    (when (is-get-request request)
      (if (= (second request) "/")
        (. output println (str (status-header :ok) (date-header) (type-header :html) "\nHello World"))
        (process-file output (nth request 1))))
    (. output println (str (status-header :bad-request) (date-header) (type-header :html) "\nNo Host: header received")))
  (. output close)))

(defn start-server []
  (create-my-server port handle-client))

;(defn start-server []
;  (doto (new java.net.ServerSocket port) (.accept)))

(defn -main
  "Start the server."
  [& args]
  (dosync (ref-set root-directory (first args)))
  (println "server started...")
  (start-server)
)
