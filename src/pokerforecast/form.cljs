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
  (value [this] "Returns the string value of the form field"))

(defprotocol IValidateInputs 
  (validation-error [this] "Returns nil or a validation error message"))

(defprotocol IUpdateState
  (state-path [this] "Returns the key path to update in the app-state")
  (update-fn [this value] "Returns the function to use to update the state-path"))

; (defn handle-change [e owner {:keys [text]}]
;   (om/set-state! owner :text (.. e -target -value)))

;; TODO: simplify, verify it's working, then build up handle-change logic
; (defn game-date-field
;   (reify
;     om/IInitState
;     (init-state [_]
;       {:text ""})
;     om/IRenderState
;     (render [this {:keys [text]]
;       (html [:span 
;              [:label "Game date"]
;              [:input {:type date :ref "game-date" :value text
;                       :onChange #(handle-change % owner state)}]]))
;     IFormField
;     (value [])
;     IValidateInputs
;     (value [])))

; (simple-form [game-date-field])

;; Takes a vector of form-field components and builds them
;; all inside a form. Each component conforms to a IFormField protocol and
;; optionally to an IValidateInputs protocol
(defn simple-form 
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
                 {:onSubmit 
                  (fn [e] 
                    (.preventDefault e)
                    ;; TODO: this is a sketch! haven't run this yet
                    (->> fields
                         (filter (partial satisfies? IValidateInputs))
                         (map (juxt identity value))
                         (filter (comp (partial satisfies? IUpdateState) first))
                         (map 
                           (fn [field value] 
                             (om/transact! app 
                               (update-path field) 
                               (partial (update-fn field) (value field)))))))}
                 (om/build-all fields)
                 [:input {:type "submit"}]]]])))))
