(ns http-server.core
  (:use [clojure.contrib.server-socket])
  (:import (java.io BufferedReader InputStreamReader PrintWriter FileReader)))

(def port 1024)

(defn process-file [out file-name]
  (try
    (with-open [reader (BufferedReader. (FileReader. file-name))]
      (doseq [line (line-seq reader)] (. out (println line))))
  (catch java.io.FileNotFoundException exception 
    (. out (println "HTTP/1.0 404 Not Found\n\nNot Found")))))

(defn header1 [status]
  (if (= status :ok)
    (str "HTTP/1.1 200 OK\n")))

(defn header2 []
  (str "Date: " (. (new java.util.Date) toString) "\n"))

(defn header3 [file-type]
  (if (= file-type :text)
    (str "Content-Type: text/html\n")))

(defn handle-client [in out]
  (println "connected")
  (def input (BufferedReader. (InputStreamReader. in)))
  (def output (new PrintWriter out))
  (def request (clojure.string/split (. input readLine) #" "))
  (if (= (first request) "GET")
    (if (= (nth request 1) "/")
      (. output println (str (header1 :ok) (header2) (header3 :text) "\nHello World"))
      (process-file output (nth request 1)))
    (. output (println "Not a GET request")))
  (. output close))


;(defn start-server []
;  (doto (new java.net.ServerSocket port) (.accept)))

(defn -main
  "I don't do a whole lot."
  [& args]
  (println "server started...")
  (def server (create-server port handle-client)))
