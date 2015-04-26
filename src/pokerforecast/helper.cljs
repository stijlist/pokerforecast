(ns pokerforecast.helper)

(defn inspect [thing] (println thing) thing)
(defn classes [& cs] (apply str (interpose " " cs)))
