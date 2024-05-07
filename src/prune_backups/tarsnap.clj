(ns prune-backups.tarsnap
  (:require [babashka.process :as p]
            [clojure.string :as str]
            [prune-backups.proto :as proto]))

(set! *warn-on-reflection* true)

(let [date-formatter (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss")]
  (defn parse-local-date [s] (java.time.LocalDateTime/parse s date-formatter)))

(defn parse-list-archive
  [s]
  (->> s
       str/split-lines
       (mapv (fn [line]
               (let [[archive datetime] (str/split line #"\t")]
                 {:archive archive, :datetime (parse-local-date datetime)})))))

(defn- tarsnap-process
  [configfile & params]
  (let [cmd (if configfile ["tarsnap" "--configfile" configfile] ["tarsnap"])
        cmd (into cmd params)]
    (p/check (p/process cmd {:out :string}))))

(defrecord TarsnapBackups [configfile]
  proto/BackupSet
    (list-backups [_]
      (->> (tarsnap-process configfile "-v" "--list-archives") :out parse-list-archive))
    (destroy-backup [_ {:keys [archive]}]
      (println "Destroying" archive)
      (tarsnap-process configfile "-d" "-f" archive)))
