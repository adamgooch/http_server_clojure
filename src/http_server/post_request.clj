(ns http-server.post-request
  (:require (http-server [headers :refer :all]
                         [util :refer (send-response)])))

(def post-content "Temporary POST Request Content")

(defn- get-script-output [root-directory requested-file]
  (let [process (.exec (java.lang.Runtime/getRuntime) (str root-directory requested-file))]
    ;I'm reading the output of the script to get the correct content length
    (with-open [reader (java.io.BufferedReader.
                       (java.io.InputStreamReader.
                       (.getInputStream process)))]
      (loop [line (.readLine reader) body line]
        (if (not (.ready reader))
          body
          (recur (str body line) (.readLine reader)))))))

(defn handle-post-request [out-stream root-directory request]
  (let [body (get-script-output root-directory request)]
    (send-response out-stream (get-headers :ok :txt body) body)))
