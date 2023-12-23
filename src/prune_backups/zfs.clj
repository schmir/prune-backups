(ns prune-backups.zfs
  (:require [clojure.string :as str]
            [prune-backups.proto :as proto]
            [babashka.process :as p]))

(let [default-zone (java.time.ZoneId/systemDefault)]
  (defn epoch-second-to-local-date
    [e]
    (let [inst (java.time.Instant/ofEpochSecond e)
          zoned (.atZone inst default-zone)
          dt (.toLocalDateTime zoned)]
      dt)))

(defn parse-snapshot-list
  [s]
  (->> s
       str/split-lines
       (mapv (fn [line]
               (let [[timestamp snapshot] (str/split line #"\t")
                     timestamp (Long/parseLong timestamp)]
                 {:timestamp timestamp
                  :archive snapshot
                  :datetime (epoch-second-to-local-date timestamp)})))))

(defn list-snapshots
  "zfs list -p -H -t snapshot -o creation,name,type,used"
  []
  (->>(p/process ["zfs" "list" "-p" "-H" "-t" "snapshot" "-o" "creation,name"] {:out :string})
      p/check
      :out
      parse-snapshot-list))

(defrecord ZFS []
  proto/BackupSet
  (list-backups [_]
    (list-snapshots))
  (destroy-backup [_ b]
    (println "deleting" (:archive b))
    (->> (p/process ["zfs" "destroy" "-r" (:archive b)])
         p/check)))
