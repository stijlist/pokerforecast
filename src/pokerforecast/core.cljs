(ns pokerforecast.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.browser.repl :as repl]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [goog.events :as events]))

(enable-console-print!)

(def app-state (atom {:games [{:date "Monday, January 18"
                                :attending [{:name "James"
                                             :rsvpd 3
                                             :attended 2}
                                            {:name "Nick"
                                             :rsvpd 1
                                             :attended 1}]
                                :hidden true}
                               {:date "Tuesday, January 19"
                                :attending [{:name "Bert"
                                             :rsvpd 2
                                             :attended 1}
                                            {:name "Max"
                                             :rsvpd 1
                                             :attended 1}]
                                :hidden true}]}))

(defn render-date
  [game]
  (dom/span #js {:className "date"}
            (:date game)))

(defn render-attending-count
  [game]
  (dom/span #js {:className "attending"}
    (count (:attending game))))

(defn render-attendee
  [attendee]
  (dom/li nil (:name attendee)))

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
        (render-attending-count game)
        (dom/button #js {:onClick (fn [e] 
                                    (put! toggle-chan (:index game)))} 
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
