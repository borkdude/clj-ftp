(ns miner.ftp-test
  (:use clojure.test
        miner.ftp)
  (:require [fs.core :as fs]
            [clojure.java.io :as io]))

(deftest listing
  (is (pos? (count (list-files "ftp://anonymous:user%40example.com@ftp.gnu.org/gnu/emacs")))))

(deftest retrieve-file-one-shot
  (let [tmp (fs/temp-file "ftp-")]
    (retrieve-file "ftp://anonymous:user%40example.com@ftp.gnu.org/gnu/emacs" "README.otherversions" tmp)
    (is (fs/exists? tmp))
    (when (fs/exists? tmp)
      (fs/delete tmp))))

(deftest get-file-client
  (let [tmp (fs/temp-file "ftp-")]
    (with-ftp [client "ftp://anonymous:user%40example.com@ftp.gnu.org/gnu/emacs"]
      (client-cd client "..")
      (is (.endsWith (client-pwd client) "gnu"))
      (is (pos? (count (client-all-names client))))
      (client-cd client "emacs")
      (is (.endsWith (client-pwd client) "emacs"))
      (client-get client "README.otherversions" tmp))
    (is (fs/exists? tmp)
    (when (fs/exists? tmp)
      (fs/delete tmp)))))

(deftest get-stream-client
  (let [tmp (fs/temp-file "ftp-")]
    (with-ftp [client "ftp://anonymous:user%40example.com@ftp.gnu.org/gnu/emacs"]
      (is (instance? java.io.InputStream
                     (client-get-stream client "README.olderversions"))))))

(deftest get-filenames
  (with-ftp [client "ftp://anonymous:user%40example.com@ftp.gnu.org/gnu/emacs"]
    (is (client-file-names client) (client-list-files client))))

(deftest get-all
  (with-ftp [client "ftp://anonymous:user%40example.com@ftp.gnu.org/gnu"]
    (is (mapv #(.getName %) (client-FTPFiles client)) (client-all-names client))))

(defn print-FTPFiles-list [label ftpfiles]
  (println)
  (println label)
  (doseq [f ftpfiles]
    (print (.getName f))
    (when (.isDirectory f) (print "/"))
    (println))
  (println))

(comment
  (with-ftp [client "ftp://anonymous:user%40example.com@ftp.gnu.org/gnu"]
    (print-FTPFiles-list "files only" (client-FTPFiles client))
    (print-FTPFiles-list "dirs only" (client-FTPFile-directories client))
    (print-FTPFiles-list "all" (client-FTPFiles-all client)))
)

;; Writable FTP server usage: http://www.swfwmd.state.fl.us/data/ftp/
(deftest write-file
  (with-ftp [client "ftp://anonymous:joe%40mailinator.com@ftp.swfwmd.state.fl.us/pub/incoming"]
    (let [source (.getFile (io/resource "sample.kml"))]
      ;;(println "write-file source = " (when source (.getFile source)))
      (client-put client source (str "s" (System/currentTimeMillis) ".kml")))))

;; Writable FTP server usage: http://cs.brown.edu/system/ftp.html
(deftest write-file2
  (with-ftp [client "ftp://anonymous:brown%40mailinator.com@ftp.cs.brown.edu/incoming"]
    (let [source (.getFile (io/resource "sample.kml"))]
      ;;(println "write-file source = " (when source (.getFile source)))
      (client-put client source (str "s" (System/currentTimeMillis) ".kml")))))
