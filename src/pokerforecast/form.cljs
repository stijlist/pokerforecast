(ns pokerforecast.form
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]))

(enable-console-print!)
(defn inspect [thing] (println thing) thing)
(def zip (partial map vector))

(defn button-to 
  ([label cb] 
   (html [:button {:onClick cb} label]))
  ([label props cb] 
   (html [:button (merge {:onClick cb} props) label])))

(defn- node-vals [owner node-names]
  (map (comp #(.-value %) (partial om/get-node owner)) node-names))

(defn- build-form-field [{:keys [name type]}] 
  (html [:div {:class "flex flow-down align-start"}
    [:label  name]
    [:input {:type type :ref name}]]))

(defn- validate-form-field [owner field]
  (if-let [validation-fn (:validation-fn field)] 
    (validation-fn (.-value (om/get-node owner (:name field))))
    true))

(defprotocol IHaveValue (value [this] "Gets the value of a form field."))
(defprotocol IValidateInput (validation-error? [this] "Returns any validation errors or nil if the field is valid."))
(defprotocol IUpdateState (update-state [this] "Updates the app state with the field's current value."))

(defn higher-order-form [form-name & fields]
  (fn [app owner]
    (reify
      om/IRender
      (render [this] 
        (html 
          [:div 
           [:form 
            {:onSubmit 
             (fn [e]
               (.preventDefault e)
               (println (validation-error? ((first fields) nil nil))) ;; evaluating the field I want to get access to its protocols seems silly
               #_(if-let [errors (map validation-error? fields)] ;; thumbs down, fields aren't reify instances with your validation-error method defined; they're functions that will eventually return the reify instances you need
                 (println errors)
                 (map update-state fields)))}
            (map om/build fields (repeat {})) ;; thumbs up, fields are all functions returning reify instances
            [:input {:type "submit"}]]])
        ))))

;; TODO: OH SHIT! this should be a higher-order componenent!
(defn- simple-form 
  "Returns a hideable form component that can update app-state. `form-name`
  is the text to be used on the button that shows and hides the form.
  `fields` is a vector of maps, containing the keys `name`, `type`, and
  `validation-fn` which are the field label, the dom input type to be used,
  and the function with which to validate user input before allowing form
  submission, respectively. Currently, validations just silently refuse output,
  but we should probably either include an option for a validation failure message,
  or scrap the simple-form mechanism and use higher-order components instead.
  `update-fn` takes a vector of values (the values in
  `fields`, ordered, at the moment the user hits submit) and the app state at
  `update-path` and returns the new app state at `update-path`."
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
                 {:class 
                  "flex flow-down align-start"
                  :onSubmit 
                  (fn [e] 
                    (.preventDefault e)
                    (if (every? (partial validate-form-field owner) fields)
                      (do
                        (om/transact! app update-path 
                          (partial update-fn 
                                   (node-vals owner (map :name fields))))
                        (om/set-state! owner :hidden true))))}
                 [:div {:class "flex flow-down align-start form-fields"}
                  (map build-form-field fields)]
                 [:input {:type "submit"}]]]])))))
