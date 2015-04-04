(ns pokerforecast.form
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]))

(defprotocol IFormField 
  (value [this] "Returns the string value of the form field")
  (update-path [this] "Returns the key path to update in the app-state")
  (update-fn [this value] "Returns the function to use to update the state-path"))

(defn simple-form 
  "Takes a vector of form-field components and builds them all inside a form. 
   Each component conforms to the IFormField protocol. On submit, we update the 
   app state at `(update-path form-field)` using `(update-fn form-field)`" 
  [form-name fields]
  (fn [app owner]
    (reify
      om/IRender
      (render [this]
        (html 
          [:form 
           {:onSubmit 
            (fn [e] 
              (.preventDefault e)
              (map 
                #(om/transact! app (update-path %) (partial update-fn %))
                fields))}
           (map #(om/build % (get app (update-path %))) fields)
           [:input {:type "submit"}]])))))
