(ns prune-backups.proto)

(defprotocol BackupSet
  (list-backups [this] "list all backups")
  (destroy-backup [this backup] "destroy the given backup"))
