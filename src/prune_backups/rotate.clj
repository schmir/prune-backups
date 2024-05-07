(ns prune-backups.rotate)

(set! *warn-on-reflection* true)

(def keep? ::keep?)
(def drop? ::drop?)

(defn minute-key
  [^java.time.LocalDateTime dt]
  (.truncatedTo dt java.time.temporal.ChronoUnit/MINUTES))

(defn hour-key [^java.time.LocalDateTime dt] (.truncatedTo dt java.time.temporal.ChronoUnit/HOURS))

(defn day-key
  [^java.time.LocalDateTime dt]
  (.. dt (truncatedTo java.time.temporal.ChronoUnit/DAYS) (toLocalDate)))

(defn week-key
  [^java.time.LocalDateTime dt]
  (.. dt
      (with (java.time.temporal.TemporalAdjusters/previousOrSame java.time.DayOfWeek/MONDAY))
      (truncatedTo java.time.temporal.ChronoUnit/DAYS)
      (toLocalDate)))

(defn month-key
  [^java.time.LocalDateTime dt]
  (.. dt
      (with (java.time.temporal.TemporalAdjusters/firstDayOfMonth))
      (truncatedTo java.time.temporal.ChronoUnit/DAYS)
      (toLocalDate)))

(defn year-key [^java.time.LocalDateTime dt] (.getYear dt))

(defn keep-first-drop-rest
  [part]
  (cons (assoc (first part) ::keep? true) (map (fn [elem] (assoc elem ::drop? true)) (rest part))))

(defn preserve*
  [p keyfn num-preserve]
  (let [partitions     (take num-preserve (partition-by keyfn (:rest p)))
        this-result    (mapcat keep-first-drop-rest partitions)
        num-considered (count this-result)]
    {:result (concat (:result p) this-result), :rest (drop num-considered (:rest p))}))

(defn drop-rest
  [p]
  {:result (concat (:result p) (mapv (fn [el] (assoc el ::drop? true)) (:rest p))), :rest nil})

(defn sort-backups [xs get-datetime] (sort-by get-datetime (fn [a b] (compare b a)) xs))

(defn rotate-backups
  [xs get-datetime & {:keys [minutely hourly daily weekly monthly yearly]}]
  (cond-> {:result nil, :rest (sort-backups xs get-datetime)}
    minutely (preserve* (comp minute-key get-datetime) minutely)
    hourly   (preserve* (comp hour-key get-datetime) hourly)
    daily    (preserve* (comp day-key get-datetime) daily)
    weekly   (preserve* (comp week-key get-datetime) weekly)
    monthly  (preserve* (comp month-key get-datetime) monthly)
    yearly   (preserve* (comp year-key get-datetime) yearly)
    true     drop-rest
    true     :result))
