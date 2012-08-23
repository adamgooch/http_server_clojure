(ns http-server.core
  (:require (http-server [server :refer :all]
                         [headers :refer :all]))
  (:import (java.io BufferedReader InputStreamReader PrintWriter FileReader DataInputStream FileInputStream))
  (:gen-class))

(def port 5000)
(def root-directory (ref ""))

(defn convert-spaces [file-name]
  (clojure.string/replace file-name "%20" " "))

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
      (with-open [input (clojure.java.io/input-stream file-name)]
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

(defn get-dir-contents [file]
  (loop [contents "" dir (.list (java.io.File. file))]
    (if (= (count dir) 0)
      (subs contents 1)
      (recur (str contents "\n" (first dir)) (rest dir)))))

(defn send-text [out-stream file-name headers]
  (let [file-path (str @root-directory (convert-spaces file-name))]
    (if (.exists (java.io.File. file-path))
      (if (.isDirectory (java.io.File. file-path))
        (do
          (send-headers out-stream (assoc headers :status (get-status-header :ok)
                                 :content-length (str "Content-Length: " (.length (get-dir-contents file-path)))))
          (send-body out-stream (get-dir-contents file-path)))
        (with-open [reader (BufferedReader.
                   (InputStreamReader.
                   (DataInputStream.
                   (FileInputStream. file-path))))]
          (send-headers out-stream (assoc headers :status (get-status-header :ok)
                                          :content-length (get-content-length-header file-path)))
          (send-text-file reader out-stream)))
      (file-not-found out-stream headers))))

(defn process-file [out-stream file-name headers]
  (if (is-media (headers :type))
    (send-media out-stream file-name headers)
    (send-text out-stream file-name headers)))

(defn echo-back-query-string [out-stream request headers]
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
  ;     (send-body out-stream (get-dir-contents (str @root-directory "/"))))
        (send-body out-stream "Hello World"))
      (let [file-path (convert-spaces (str @root-directory request))]
        (process-file out-stream request
                    (assoc headers :type (get-type-header (get-file-type request))))))))

(defn handle-post-request [out-stream request headers]
  (send-headers out-stream (assoc headers :status (get-status-header :ok)
                                  :content-length (str "Content-Length: " (.length "POST Request"))))
  (send-body out-stream "POST Request"))

(defn handle-put-request [out-stream request headers]
  (send-headers out-stream (assoc headers :status (get-status-header :ok)
                                  :content-length (str "Content-Length: " (.length "PUT Request"))))
  (send-body out-stream "PUT Request"))

(defn serve-client [out-stream request headers]
    (cond
      (= (first request) "GET")
        (handle-get-request out-stream (second request) headers)
      (= (first request) "PUT")
        (handle-put-request out-stream (second request) headers)
      (= (first request) "POST")
        (handle-post-request out-stream (second request) headers)))

(defn send-no-host-header [out-stream headers]
  (send-headers out-stream (assoc headers :status (get-status-header :bad-request)
                                :content-length (str "Content-Length: " (.length "No Host: header received"))))
  (send-body out-stream "No Host: header recevied"))

(defn get-request [in-stream]
  (with-open [w (clojure.java.io/writer "/Users/Tank/temp/server.txt" :append true)]
    (let [input (BufferedReader. (InputStreamReader. in-stream))]
      (loop [received (list (.readLine input)) cnt 10]
        (if (not= (first received) "")
          (do
            (.write w (str (first received) "\n"))
            (recur (conj received (.readLine input)) (dec cnt)))
          received)))))

(defn handle-client [in-stream out-stream]
  (let [request (get-request in-stream)
        headers {:date (get-date-header), :type (get-type-header "txt"), :blank-line ""}]
      (if (empty? (filter #(re-find #"Host: " %) request))
        (send-no-host-header out-stream headers)
        (serve-client out-stream (clojure.string/split (last request) #" ") headers))))

(defn start-server []
  (create-server handle-client (java.net.ServerSocket. port)))

(defn -main
  "Start the server."
  [& args]
  (dosync (ref-set root-directory (first args)))
;  (println "server started...")
  (start-server))
