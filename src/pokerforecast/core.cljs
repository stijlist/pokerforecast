(ns pokerforecast.core
  (:require [clojure.browser.repl :as repl]
            [goog.dom :as dom]
            [goog.events :as events]
            [goog.cssom :as css]))

;; (repl/connect "http://localhost:9000/repl")

(enable-console-print!)


(defn toggle-hide
  [element-or-id]
  (-> (dom/getElement element-or-id)
      (.-classList)
      (.toggle "hide")))

(defn expand-clicked-div
  [event]
  (toggle-hide (dom/getNextElementSibling (.-target event))))

(-> (dom/getElement "login")
    (events/listen "click" #(toggle-hide "login-form-container")))

(-> (dom/getElementByClass "attending")
    (events/listen "click" expand-clicked-div))

(println (dom/getElement "login-form-container"))
(println "Hello world!")
