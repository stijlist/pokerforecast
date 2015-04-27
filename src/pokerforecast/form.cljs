(ns pokerforecast.form
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]))

(enable-console-print!)
(defn inspect [thing] (println thing) thing)

(defn button-to 
  ([label cb] 
   (html [:button {:onClick cb} label]))
  ([label props cb] 
   (html [:button (merge {:onClick cb} props) label])))

(defprotocol Field 
  (value [this] "Gets the value of a form field.")
  (validation-error? [this] "Returns any validation errors or nil if the field is valid.")
  (update-state [this] "Updates the app state with the field's current value."))

(defn higher-order-form [form-name & fields]
  (fn [app owner]
    (reify
      om/IRender
      (render [this] 
        (html 
          [:div 
           [:span form-name]
           [:form 
            {:onSubmit 
             (fn [e]
               (.preventDefault e)
               (let [errors (filter identity (map validation-error? fields))]
                 (if (not-empty errors)
                   (println errors)
                   (map update-state fields))))}
            (map om/build fields (repeat {}))
            [:input {:type "submit"}]]])))))
