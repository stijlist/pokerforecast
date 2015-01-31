(ns pokerforecast.core
  (:require [clojure.browser.repl :as repl]
            [om.dom :as dom]
            [om.core :as om]
            [goog.events :as events]))

(enable-console-print!)


(def app-state (atom {:games [{:date "Monday, January 18"
                               :attending [{:name "James"
                                            :rsvpd 3
                                            :attended 2}
                                           {:name "Nick"
                                            :rsvpd 1
                                            :attended 1}]}]}))

(println @app-state)

(om/root 
  (fn [app owner]
    (reify om/IRender
      (render [_]
        (dom/h1 nil "hello world"))))
  app-state
  {:target (. js/document getElementById "game-list")})
