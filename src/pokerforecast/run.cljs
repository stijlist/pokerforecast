(ns pokerforecast.run
  (:require [om.core :as om :include-macros true]
            [pokerforecast.view :as view]
            [pokerforecast.state :refer [app-state]]))

#_(om/root
  view/account-buttons
  app-state
  {:target (. js/document getElementById "account-buttons")})

(om/root 
  view/app
  app-state
  {:target (. js/document getElementById "app")})
