(ns http-server.server
  (:import (java.net ServerSocket Socket SocketException)))

(defn- on-thread [function]
  (doto (Thread. #^Runnable function)
    (.start)))

(defn- close-socket [#^Socket socket]
  (when-not (.isClosed socket)
    (doto socket
      (.shutdownInput)
      (.shutdownOutput)
      (.close))))

(defn- accept-fn [#^Socket socket connections function]
  (let [ins (.getInputStream socket)
        outs (.getOutputStream socket)]
    (on-thread #(do
                  (dosync (commute connections conj socket))
                  (try
                    (function ins outs)
                  (catch SocketException e))
                  (close-socket socket)
                  (dosync (commute connections disj socket))))))

(defstruct server-def :server-socket :connections)

(defn create-server-aux [function #^ServerSocket socket]
  (let [connections (ref #{})]
    (on-thread #(when-not (.isClosed socket)
                  (try
                    (accept-fn (.accept socket) connections function)
                  (catch SocketException e))
                  (recur)))
    (struct-map server-def :server-socket socket :connections connections)))

(defn create-my-server [port function]
  (create-server-aux function (ServerSocket. port)))

;(defn start-server []
;  (doto (new java.net.ServerSocket port) (.accept)))

