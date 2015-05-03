(ns pokerforecast.form
  (:require [om.core :as om :include-macros true]
            [sablono.core :refer-macros [html]]))

(enable-console-print!)
(defn inspect [thing] (println thing) thing)

(defn button-to 
  ([label cb] 
   (html [:button {:onClick cb} label]))
  ([label props cb] 
   (html [:button (merge {:onClick cb} props) label])))

(defprotocol Field 
  (value [this data owner] "Gets the value of a form field.")
  (validation-error? [this data owner] "Returns any validation errors or nil if the field is valid.")
  (update-state [this data owner value] "Updates the app state with the field's current value."))

(defn form [form-name & fields]
  (fn [data owner]
    (om/component
      (html 
        [:div 
         [:span form-name]
         [:form 
          {:onSubmit 
           (fn [e]
             (.preventDefault e)
             (let [errors (filter identity (map #(validation-error? % data owner) fields))]
               (if (not-empty errors)
                 (println errors)
                 (map #(update-state % data owner (value % data owner)) fields))))}
          (map om/build fields (repeat {}))
          [:input {:type "submit"}]]]))))
