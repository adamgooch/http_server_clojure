(ns http-server.util)

(defn send-response [out-stream headers content]
  (let [writer (clojure.java.io/writer out-stream)]
    (doseq [line headers]
      (.write writer (str (second line) "\n")))
    (when (not (nil? content))
      (.write writer content))
    (.flush writer)))

(def sent-log-file "/Users/Tank/temp/server_sent_log.txt")
(def request-log-file "/Users/Tank/temp/server_received_headers_log.txt")

(defn log-request [request]
  (with-open [writer (clojure.java.io/writer request-log-file :append true)]
    (doseq [line request]
      (.write writer (str line "\n")))))

(defn log-send-message [headers contents]
  (with-open [writer (clojure.java.io/writer sent-log-file :append true)]
    (doseq [line headers]
      (.write writer (str line "\n")))
    (.write writer contents)))
