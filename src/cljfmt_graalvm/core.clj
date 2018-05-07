(ns cljfmt-graalvm.core
  (:require [cljfmt.core :as fmt]
            [clojure.java.io :as io]
            [clojure.tools.cli :as cli])
  (:gen-class))

(defn exists [path]
  (-> path
      (io/as-file)
      (.exists)))

(defn is-directory [path]
  (-> path
      (io/as-file)
      (.isDirectory)))

(defn is-file [path]
  (-> path
      (io/as-file)
      (.isFile)))

(def cli-config
  [["-p" "--path PATH" "Path to folder with clojure project source code"
    :default "."
    :validate [exists "Sources path does not exist"
               is-directory "Sources path should refer to directory"]]
   ["-c" "--config CONFIG" "Path to config file"
    :validate [exists "Config path does not exist"
               is-file "Config path should refer to file"]]
   ["-h" "--help"]])

(defn grep [re dir]
  (filter
    (comp (partial re-find re)
          (fn [v] (.getPath (io/file v))))
    (file-seq dir)))

(defn file-pattern [opts]
  (get opts :file-pattern #"\.clj[sx]?$"))

(defn find-files [opts f]
  (let [f (io/file f)]
    (if (.isDirectory f)
      (grep (file-pattern opts) f)
      [f])))

(defn reformat [opts file]
  (let [original (slurp file)
        revised (fmt/reformat-string original opts)]
    (when (not= original revised)
      (spit file revised))))

(defn process [opts files]
  (->> files
       (map (partial reformat opts))
       (doall)))

(defn -main [& args]
  (let [{:keys [options summary]} (cli/parse-opts args cli-config)
        {:keys [path config help]} options
        opts (or (some-> config slurp read-string) {})]
    (some->> path (println "Path: "))
    (some->> config (println "Config: "))
    (if help
      (println summary)
      (->> path
           (find-files opts)
           (process opts)))))
