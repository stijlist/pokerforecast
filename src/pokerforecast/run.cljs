(ns pokerforecast.run
  (:require [om.core :as om :include-macros true]
            [pokerforecast.view :as view]
            [pokerforecast.state :refer [app-state]]))

(om/root 
  view/app
  app-state
  {:target (. js/document getElementById "app")})
