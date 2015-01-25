(ns pokerforecast.core
  (:require [clojure.browser.repl :as repl]
            [goog.dom :as dom]
            [goog.events :as events]
            [goog.cssom :as css]))

;; (repl/connect "http://localhost:9000/repl")

(enable-console-print!)

(def mydata #js [4 0 2 5 0 0 6 0])

(.. js/d3 
    (select (dom/getElement "graph"))
    (data mydata)
    (enter)
    (insert "span")
    (style "height" #(str % "em"))
    (style "background-color" "steelblue")
    (text identity))

(defn toggle-hide
  [element-or-id]
  (-> (dom/getElement element-or-id)
      (.-classList)
      (.toggle "hide")))

(defn expand-sibling-div
  [event]
  (toggle-hide (dom/getNextElementSibling (.-target event))))

(-> (dom/getElement "login")
    (events/listen "click" #(toggle-hide "login-form-container")))

(-> (dom/getElementByClass "attending")
    (events/listen "click" expand-sibling-div))
