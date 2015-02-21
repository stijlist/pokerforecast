(ns pokerforecast.view
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [pokerforecast.core :as forecast]
            [pokerforecast.state :refer [app-state]]))

(defn- inspect [thing] (println thing) thing)

(defn- hide-if [hidden?]
  (if hidden? "hide" ""))

(defn- classes [& cs]
  (apply str (interpose " " cs)))

(defn- as-percentage [n] 
  (.toFixed (* n 100)))

(defn- render-date
  [game]
  (html [:span {:class "date"} (:date game)]))

(defn- render-likelihood
  [game]
  (html [:span {:class "likelihood"}
         (as-percentage 
           (forecast/maximum-game-likelihood game))]))

(defn- render-attending-count
  [game]
  (html [:span {:class "attending"}
    (count (:attending game))]))

(defn- render-player
  [attendee]
  (html [:li 
    [:span {:class "attendee"} (:name attendee)] 
    (if (> (:rsvpd attendee) 0)
      [:span {:class "flake-rate"}
       (as-percentage (forecast/flake-rate attendee))]
      [:span {:class "no-flake-rate"}])]))

(defn- render-attendees
  [game hidden]
  (html [:ul {:class (classes "attendees" (hide-if hidden))}
    (map render-player (:attending game))]))

(defn- currently-attending [{:keys [attending current-user]}]
  (some #{current-user} attending))

(defn- game-view 
  [{:keys [game]} owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [hidden]}]
      (html [:li {:class (if (currently-attending game) "in-attendance")}
             (render-date game) 
             (render-likelihood game)
             (render-attending-count game)
             [:button {:onClick #(om/update-state! owner :hidden not)} 
              "Show attending"]
             (render-attendees game hidden)]))))

(defn- with-players [players games]
  (map 
    #(update-in % [:attending] (partial mapv (partial get players))) 
    games))

(defn- with-current-user [user games]
  (map #(assoc % :current-user user) games))

(defn- fresh-player [new-name email threshold]
  (assoc {:attended 0 :rsvpd 0} :name new-name :threshold threshold))

(defn- add-player [player existing]
  (let [next-id (inc (apply max (keys existing)))]
    (assoc existing next-id player)))

(defn- add-game
  [date existing]
  (conj existing {:date date :players [] :hidden true}))

(defn- game-list
  [{:keys [players games logged-in-user] :as app} owner]
  (reify
    om/IRenderState
    (render-state [this state]
      (html 
        [:ul 
         (om/build-all game-view 
           ;; TODO: collect multiple cursors in a map and pass them to game-view
           ;; instead of associng new data with each game ad-hoc
           (->> games 
                (with-players players) 
                (with-current-user logged-in-user) 
                (map (partial hash-map :game))) 
           {:init-state {:hidden true}})]))))

(defn- node-vals [owner node-names]
  (map (comp #(.-value %) (partial om/get-node owner)) node-names))

(defn- build-form-field
  [{:keys [field-name field-type]}] 
  (html [:span 
    [:label field-name]
    [:input {:type field-type :ref field-name}]]))

(defn- simple-form 
  "Returns a generic hideable form component that can update app-state.
  `form-name` is the text to be used on the button that shows and hides the form.
  `fields` is a vector of maps, containing the keys `field-name` and `field-type`, 
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
               [:div {:class (hide-if hidden)}
                [:form 
                 {:onSubmit 
                  (fn [e] 
                    (.preventDefault e)
                    (om/transact! app update-path 
                      (partial update-fn 
                               (node-vals owner (map :field-name fields))))
                    (om/set-state! owner :hidden true))}
                 (for [field fields] 
                   (build-form-field field))
                 [:input {:type "submit"}]]]])))))

(def login-form 
  (simple-form "Login" [{:field-name "email" :field-type "text"}]
               :logged-in-user (fn [[email] current-user] 
                                 (first (filter #(= (:email %) email)
                                                (vals (:players @app-state)))))))

(def new-player-form
  (simple-form "Create account" [{:field-name "Name" :field-type "text"}
                                 {:field-name "Email" :field-type "text"}
                                 {:field-name "Minimum game threshold" :field-type "number"}]
               :players (fn [[pname email threshold] existing] 
                          (add-player (fresh-player pname email threshold) existing))))

(def new-game-form 
  (simple-form "New game" [{:field-name "Date" :field-type "text"}]
               :games (fn [[date] games] (add-game date games))))

(defn- player-list
  [app owner]
  (om/component
    (html [:div 
           [:h3 "Registered Players"]
           [:div (map render-player (vals (:players app)))]])))

(defn- logged-in-user [app owner]
  (om/component
    (html 
      (if-let [user (:logged-in-user app)]
        [:div [:span {:class "current-user"} (:name user)]]
        [:div [:span {:class "no-current-user"}]]))))

(defn render-app
  [app owner]
  (om/component
    (html [:div 
           (om/build logged-in-user app)
           (om/build new-player-form app {:init-state {:hidden true}})
           (om/build new-game-form app {:init-state {:hidden true}})
           (om/build login-form app {:init-state {:hidden true}})
           (om/build game-list app)
           (om/build player-list app)])))
