(ns pokerforecast.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.browser.repl :as repl]
            [pokerforecast.input]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [cljs.core.async :refer [put! chan <!]]))

(enable-console-print!)

(defn inspect [thing] (println thing) thing)

(def app-state (atom {:games [{:date "Monday, January 18"
                                :attending [{:name "James"
                                             :rsvpd 3
                                             :attended 2
                                             :threshold 3}
                                            {:name "Nick"
                                             :rsvpd 1
                                             :attended 1
                                             :threshold 4}]
                                :hidden true}
                               {:date "Tuesday, January 19"
                                :attending [{:name "Bert"
                                             :rsvpd 2
                                             :attended 1
                                             :threshold 2}
                                            {:name "Max"
                                             :rsvpd 1
                                             :attended 1
                                             :threshold 3}
                                            {:name "James"
                                             :rsvpd 3
                                             :attended 2
                                             :threshold 3}]
                                :hidden true}]}))

(defn attendance-rate
  [{:keys [attended rsvpd]}]
  (/ attended rsvpd))

(defn powerset
  [coll]
  (if-not (seq coll) [[]] ; maybe use empty set instead?
    (let [rest-powerset (powerset (rest coll))]
      (concat (map #(conj % (first coll)) rest-powerset) rest-powerset))))

(defn game-likelihood
  [attendees]
  (->> attendees
       (map attendance-rate)
       (reduce *)))

(defn all-thresholds-satisfied [attendees]
  (>= 
    (count attendees)
    (apply max (map :threshold attendees))))

(defn maximum-game-likelihood 
  [{:keys [attending]}]
  (->> (powerset attending)
       (filter all-thresholds-satisfied)
       (map game-likelihood)
       (apply max)))

;; only special-casing nil right now because max-game-likelihood can return nil
(defn as-percentage [n] 
  (if n (.toFixed (* n 100)) 0))

(defn flake-rate [attendee]
  (- 1 (attendance-rate attendee)))

(defn render-date
  [game]
  (dom/span #js {:className "date"}
            (:date game)))

(defn render-likelihood
  [game]
  (dom/span #js {:className "likelihood"}
            (as-percentage (maximum-game-likelihood game))))

(defn render-attending-count
  [game]
  (dom/span #js {:className "attending"}
    (count (:attending game))))

(defn render-attendee
  [attendee]
  (dom/li nil 
          (dom/span #js {:className "attendee"} (:name attendee)) 
          (dom/span #js {:className "flake-rate"}
                    (as-percentage (flake-rate attendee)))))

(defn render-attendees
  [game]
  (apply dom/ul #js {:className (str "attendees" (if (:hidden game) " hide" ""))}
         (map render-attendee (:attending game))))

(defn game-view 
  [game owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [toggle-chan]}]
      (dom/li nil
        (render-date game) 
        (render-likelihood game)
        (render-attending-count game)
        (dom/button #js {:onClick #(put! toggle-chan (:index game))} 
                    "Show attending")
        (render-attendees game)))))

(defn- assoc-with-indices [coll]
  (map-indexed (fn [i item] (assoc item :index i)) coll))

(defn render-game-list
  [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:toggle-chan (chan)})
    om/IWillMount
    (will-mount [this]
      (let [toggle-chan (om/get-state owner :toggle-chan)]
        (go 
          (loop []
            (let [game-index (<! toggle-chan)]
              (om/transact! app [:games game-index :hidden] not))
            (recur)))))
    om/IRenderState
    (render-state [this {:keys [toggle-chan]}]
      (apply dom/ul nil 
            (om/build-all game-view 
                          (assoc-with-indices (:games app))
                          {:init-state {:toggle-chan toggle-chan}})))))

(om/root 
  render-game-list
  app-state
  {:target (. js/document getElementById "app")})
