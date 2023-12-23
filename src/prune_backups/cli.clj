(ns prune-backups.cli
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [prune-backups.rotate :as rotate]
            [prune-backups.proto :as proto]
            [prune-backups.tarsnap :as tarsnap]
            [prune-backups.zfs :as zfs]
            [babashka.cli :as cli])
  (:gen-class))

(set! *warn-on-reflection* true)


(def spec
  {:config
   {:desc "Path to edn config file"
    :require true}
   :help
   {:desc "Show help message"
    :alias :h}})

(defn print-help
  []
  (println "Usage: rotate-backups.clj [options]")
  (println (cli/format-opts {:spec spec :order [:gen :install-gen :run-ninja :help]}))
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
        (map (fn [prefix]
               (find-archives-with-prefix prefix all-archives)))
        (mapcat (fn [archives]
                  (rotate/rotate-backups archives :datetime (:rotate cfg)))))))

(defn run
  [bs]
  (println (pr-str bs))
  (let [archives (rotate bs)
        destroy (filter rotate/drop? archives)]
    (println (count archives) "archives, destroying" (count destroy))
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
    (let [{:keys [help config]} (cli/parse-opts args
                                                {:spec spec
                                                 :restrict (keys spec)})]
      (when help
        (print-help))

      (->> config slurp (edn/read-string custom-readers) run))
    (System/exit 0)
    (finally
      (shutdown-agents))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
