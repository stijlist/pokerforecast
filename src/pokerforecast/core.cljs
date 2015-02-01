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
                               :index 0 ; TODO: use map-indexed at call site instead 
                               :hidden true}
                              {:date "Tuesday, January 19"
                               :attending [{:name "Bert"
                                            :rsvpd 2
                                            :attended 1}
                                           {:name "Max"
                                            :rsvpd 1
                                            :attended 1}]
                               :index 1
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

(def toggle-chan (chan))

(put! toggle-chan "toggle-chan is working")
(go
  (println (<! toggle-chan)))

(defn game-view 
  [game owner]
  (reify
    om/IRenderState
    (render-state [this state]
      (dom/li nil
        (render-date game) 
        (render-attending-count game)
        (dom/button #js {:onClick (fn [e] 
                                    (println "Click triggered!") 
                                    (put! toggle-chan (:index game)))} 
                    "Show attending")
        (render-attendees game)))))

(defn render-game-list
  [app owner]
  (reify
    #_om/IInitState
    #_(init-state [_]
      (println "state initialized")
      {:toggle-chan (chan)})
    om/IWillMount
    (will-mount [_]
      (go 
        (loop []
          (println "go-loop started")
          (let [toggled-game-index (<! toggle-chan)]
            (println (str "Index is " toggled-game-index))
            (om/transact! app [:games toggled-game-index :hidden] not))
          (recur))))
    om/IRenderState
    (render-state [this state]
      (apply dom/ul nil 
            (om/build-all game-view (:games app))))))

(om/root 
  render-game-list
  app-state
  {:target (. js/document getElementById "app")})
