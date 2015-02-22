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

(defn- build-form-field
  [{:keys [name type]}] 
  (html [:span 
    [:label name]
    [:input {:type type :ref name}]]))

(defn- simple-form 
  "Returns a hideable form component that can update app-state.
  `form-name` is the text to be used on the button that shows and hides the form.
  `fields` is a vector of maps, containing the keys `name` and `type`, 
  which are the field label and the dom input type to be used, respectively.
  `update-fn` takes a vector of values (the values in `fields`, ordered, at the 
  moment the user hits submit) and the app state at `update-path` and returns 
  the new app state at `update-path`."
  [form-name fields update-path update-fn]
  (fn [app owner]
    (reify
      om/IRenderState
      (render-state [this {:keys [hidden] :as state}]
        (html [:div 
               [:button {:onClick #(om/update-state! owner :hidden not)}
                form-name]
               [:div {:class (if hidden "hide" "")}
                [:form 
                 {:onSubmit 
                  (fn [e] 
                    (.preventDefault e)
                    (om/transact! app update-path 
                      (partial update-fn 
                               (node-vals owner (map :name fields))))
                    (om/set-state! owner :hidden true))}
                 (map build-form-field fields)
                 [:input {:type "submit"}]]]])))))
