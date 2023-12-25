(ns prune-backups.cli
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [prune-backups.meta :as meta]
            [prune-backups.rotate :as rotate]
            [prune-backups.proto :as proto]
            [prune-backups.tarsnap :as tarsnap]
            [prune-backups.zfs :as zfs]
            [babashka.cli :as cli])
  (:gen-class))

(set! *warn-on-reflection* true)


(def spec
  {:config
   {:desc "Path to edn config file"}
   :version
   {:desc "Show version information"}
   :help
   {:desc "Show help message"
    :alias :h}})

(defn print-help
  []
  (println "Usage: prune-backups [options]")
  (println (cli/format-opts {:spec spec :order [:config :version :help]}))
  (System/exit 0))

(defn find-archives-with-prefix
  [prefix archives]
  (filter (fn [a] (str/starts-with? (:archive a) prefix))
          archives))

(defn rotate
  ([cfg]
   (rotate cfg (proto/list-backups (:backup-set cfg))))
  ([cfg all-archives]
   (->> (:prefixes cfg)
        (mapcat (fn [prefix]
                  (let [archives (find-archives-with-prefix prefix all-archives)
                        res (rotate/rotate-backups archives :datetime (:rotate cfg))]
                    (println (str (pr-str prefix) ": " (count res) " archives, destroying " (count (filter rotate/drop? res))))
                    res))))))

(defn run
  [bs]
  (println (pr-str bs))
  (let [archives (rotate bs)
        destroy (filter rotate/drop? archives)]
    ;; (println (count archives) "archives, destroying" (count destroy))
    ;; (pprint/pprint archives)
    (doseq [d destroy]
      (proto/destroy-backup (:backup-set bs) d))))

(defn tarsnap-reader
  [m]
  (-> m
      (dissoc :configfile)
      (assoc :backup-set (tarsnap/->TarsnapBackups (:configfile m)))))

(defn zfs-reader
  [m]
  (-> m
      (assoc :backup-set (zfs/->ZFS))))

(def custom-readers
  {:readers {'prune-backups/tarsnap tarsnap-reader
             'prune-backups/zfs zfs-reader}})

(defn -main [& args]
  (try
    (let [{:keys [help version config]} (cli/parse-opts args
                                                        {:spec spec
                                                         :restrict (keys spec)})]
      (when version
        (println meta/version)
        (System/exit 0))
      (when help
        (print-help))

      (->> config slurp (edn/read-string custom-readers) run))
    (System/exit 0)
    (finally
      (shutdown-agents))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
