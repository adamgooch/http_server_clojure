(ns http-server.headers
  (:import (java.util Date)))

(defn- get-status-header [status]
  (cond (= status :ok) "HTTP/1.1 200 OK"
        (= status :not-found) "HTTP/1.1 404 Not Found"
        (= status :bad-request) "HTTP/1.1 400 Bad Request"))

(def get-date-header (str "Date: " (.toString (Date.))))

(defn- get-content-length-header [content]
  (str "Content-Length: " (.length content)))

(defn- get-type-header [file-type]
  (cond (= file-type :html) "Content-Type: text/html"
        (= file-type :css) "Content-Type: text/css"
        (= file-type :jpg) "Content-Type: image/jpg"
        (= file-type :gif) "Content-Type: image/gif"
        (= file-type :png) "Content-Type: image/png"
        :else "Content-Type: text/plain"))

(defn get-headers [status content-type content]
  {:status (get-status-header status)
   :date get-date-header
   :type (get-type-header content-type)
   :content-length (get-content-length-header content)
   :blank-line ""})
