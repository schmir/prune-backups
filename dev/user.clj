(ns user
  #_:clj-kondo/ignore
  (:require [clojure.test]
            [clojure.tools.namespace.repl :as nsrepl]
            [kaocha.repl]))

(clojure.tools.namespace.repl/set-refresh-dirs "src" "test" "dev")
;; (clojure.tools.namespace.repl/disable-reload!)

(def refresh nsrepl/refresh)
(def refresh-all nsrepl/refresh-all)

(defn run-tests
  []
  (nsrepl/refresh)
  (kaocha.repl/run-all)
  ;;(clojure.test/run-all-tests #"fin\..*")
)
