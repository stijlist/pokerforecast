(ns pokerforecast.core)

(enable-console-print!)

(defn- attendance-rate
  [{:keys [attended rsvpd]}]
  (if (= 0 rsvpd) nil (/ attended rsvpd)))

(defn- powerset
  [coll]
  (if-not (seq coll) 
    [[]]
    (let [rest-powerset (powerset (rest coll))]
      (concat (map #(conj % (first coll)) rest-powerset) rest-powerset))))

(defn- game-likelihood
  [attendees]
  (->> attendees
       (map attendance-rate) ; what do we do with unknown attendance rates?
       (filter (comp not nil?))
       (reduce *)))

(defn- enough-players [attendees]
  (>= 
    (count attendees)
    (apply max (map :threshold attendees))))

(defn- default [d input]
  (if (nil? input) d input))

(defn maximum-game-likelihood 
  [{:keys [attending]}]
  (->> (powerset attending) ; all combinations of folks that might show up
       (filter enough-players)
       (map game-likelihood)
       (apply max)
       (default 0)))

(defn flake-rate [attendee]
  (- 1 (attendance-rate attendee)))
