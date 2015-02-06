(ns pokerforecast.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.browser.repl :as repl]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [goog.string.format]
            [goog.events :as events]))

(enable-console-print!)

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
                                             :threshold 5}
                                            {:name "James"
                                             :rsvpd 3
                                             :attended 2
                                             :threshold 3}]
                                :hidden true}]}))

(defn attendance-rate
  [{:keys [attended rsvpd]}]
  (/ attended rsvpd))

(defn powerset ; TODO: write this damn function
  [coll]
  ; powerset. write it recursively. take a small step. 
  ; if, magically, you have the powerset of a collection already, 
  ; what would you need to do to get the powerset of (add new-item collection)?
  ; you'd need to map over the old powerset, appending the new item to each 
  ; of the subsets, and append the result of that map to the old powerset. OH!
  ; ok, I can write this. 
  (if-not (seq coll) [[]] ; maybe use empty set instead?
    (let [old-powerset (powerset (rest coll))] ; TODO: rename to rest-powerset
      (concat (map #(conj % (first coll)) old-powerset) old-powerset))))

(println (powerset [1 2]))

(defn game-likelihood
  [game]
  (->> (:attending game)
       (map attendance-rate)
       (reduce *)))

(defn two-decimals [n] (goog.string.format "%.2f" n))

(defn render-date
  [game]
  (dom/span #js {:className "date"}
            (:date game)))

(defn render-likelihood
  [game]
  (dom/span #js {:className "likelihood"}
            (two-decimals (game-likelihood game))))

(defn render-attending-count
  [game]
  (dom/span #js {:className "attending"}
    (count (:attending game))))

(defn render-attendee
  [attendee]
  (dom/li nil 
          (:name attendee) 
          (dom/span #js {:className "flake-rate"}
                    (two-decimals (attendance-rate attendee)))))

(defn render-attendees
  [game]
  ; TODO: check goog/css for classname manipulation utils
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
      (println "state initialized")
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
