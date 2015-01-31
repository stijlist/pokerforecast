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
(defn render-game-list
  [app owner]
  (om/component
    (dom/h1 nil "hello components!")))

(om/root 
  render-game-list
  app-state
  {:target (. js/document getElementById "game-list")})
