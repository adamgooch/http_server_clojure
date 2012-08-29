(ns http-server.server
  (:import (java.net ServerSocket Socket SocketException)))

(defn- new-thread [function]
  (.start (Thread. #^Runnable function)))

(defn- close-socket [#^Socket socket]
  (when-not (.isClosed socket)
    (doto socket
      (.shutdownInput)
      (.shutdownOutput)
      (.close))))

(defn- handle-connection [#^Socket socket function]
  (let [in-stream (.getInputStream socket)
        out-stream (.getOutputStream socket)]
    (new-thread #(do
                  (try
                    (function in-stream out-stream)
                  (catch SocketException e))
                    ;If something should fail in the communication, let it die.
                  (close-socket socket)))))

(defn create-server [function #^ServerSocket socket]
  "The function required here is sent the input and output
  stream of the connected client to handle the communication."
  (new-thread #(when-not (.isClosed socket)
                (try
                  (handle-connection (.accept socket) function)
                (catch SocketException e))
                  ;socket has been closed while waiting for a connection - this is expected.
                (recur)))
  socket)
