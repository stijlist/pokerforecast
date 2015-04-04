(ns pokerforecast.form
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]))

(defn button-to 
  ([label cb] 
   (html [:button {:onClick cb} label]))
  ([label props cb] 
   (html [:button (merge {:onClick cb} props) label])))

(defn- node-vals [owner node-names]
  (map (comp #(.-value %) (partial om/get-node owner)) node-names))

(defn- build-form-field [{:keys [name type]}] 
  (html [:span 
    [:label name]
    [:input {:type type :ref name}]]))

(defn- validate-form-field [owner field]
  (if-let [validation-fn (:validation-fn field)] 
    (validation-fn (.-value (om/get-node owner (:name field))))
    true))

(defprotocol IFormField 
  (value [this] "Returns the string value of the form field")
  (validation-error [this] "Returns nil or a validation error message")
  (update-path [this] "Returns the key path to update in the app-state")
  (update-fn [this value] "Returns the function to use to update the state-path"))

(defn display-errors [_])

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
              (if-let [[& errs] (keep (map validation-error fields))]
                (display-errors errs)
                (map 
                  (fn [field]
                    (om/transact! app 
                                  (update-path field) 
                                  (partial update-fn field (value field))))
                  fields)))}
           (map #(om/build % (get app (update-path %))) fields)
           [:input {:type "submit"}]])))))
