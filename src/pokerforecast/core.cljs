(ns pokerforecast.core
  (:require [clojure.browser.repl :as repl]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [goog.events :as events]))

(enable-console-print!)

(def app-state (atom {:games [{:date "Monday, January 18"
                               :attending [{:name "James"
                                            :rsvpd 3
                                            :attended 2}
                                           {:name "Nick"
                                            :rsvpd 1
                                            :attended 1}]}]}))

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
  (apply dom/ul #js {:className "attendees"}
         (map render-attendee (:attending game))))

(defn game-view [game]
  (om/component
    (dom/li nil
      (render-date game) 
      (render-attending-count game)
      (render-attendees game))))

(defn render-game-list
  [app owner]
  (om/component
    (apply dom/ul nil 
           (om/build-all game-view (:games app)))))

(om/root 
  render-game-list
  app-state
  {:target (. js/document getElementById "app")})
