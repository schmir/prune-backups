(ns prune-backups.tarsnap
  (:require [clojure.string :as str]
            [prune-backups.proto :as proto]
            [babashka.process :as p]))

(set! *warn-on-reflection* true)

(let [date-formatter (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss")]
  (defn parse-local-date
    [s]
    (java.time.LocalDateTime/parse s date-formatter)))

(defn parse-list-archive
  [s]
  (->> s
       str/split-lines
       (mapv (fn [line]
               (let [[archive datetime] (str/split line #"\t")]
                 {:archive archive
                  :datetime (parse-local-date datetime)})))))

(defrecord TarsnapBackups [configfile]
  proto/BackupSet
  (list-backups [_]
    (->> (p/process ["tarsnap" "-v" "--configfile" configfile "--list-archives"]
                    {:out :string})
         p/check
         :out
         parse-list-archive))
  (destroy-backup [_ {:keys [archive]}]
    (println "Destroying" archive)
    (p/check (p/process ["tarsnap" "--configfile" configfile "-d" "-f" archive]))))
