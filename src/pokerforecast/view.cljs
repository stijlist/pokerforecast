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

(defn- render-player
  [attendee]
  (html [:li 
    [:span {:class "attendee"} (:name attendee)] 
    (if (> (:rsvpd attendee) 0)
      [:span {:class "flake-rate"}
       (as-percentage (forecast/flake-rate attendee))]
      [:span {:class "no-flake-rate"}])]))

(defn- join-players [players ids]
  (mapv (partial get players) ids))

(defn- game-view 
  [{:keys [game players current-user]} owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [hidden]}]
      (let [attending (join-players players (:attending game))]
        (html [:li {:class (if (some #{current-user} (:attending game)) "in-attendance")}
               [:span {:class "date"} (:date game)]
               [:span {:class "likelihood"}
                        (as-percentage 
                          (forecast/maximum-game-likelihood attending))]
               [:span {:class "attending-count"} (count attending)]
               [:button {:onClick #(om/update-state! owner :hidden not)} 
                "Show attending"]
               [:ul {:class (classes "attendees" (hide-if hidden))} 
                (map render-player attending)]])))))

(defn- fresh-player [new-name email threshold]
  (assoc {:attended 0 :rsvpd 0} :name new-name :threshold threshold))

(defn- add-player [player existing]
  (let [next-id (inc (apply max (keys existing)))]
    (assoc existing next-id player)))

(defn- add-game
  [date existing]
  (conj existing {:date date :players [] :hidden true}))

(defn- game-list
  [{:keys [players games current-user] :as app} owner]
  (om/component
    (html 
      [:ul 
       (om/build-all game-view 
         (map (partial hash-map :players players :current-user current-user :game) games)
         {:init-state {:hidden true}})])))

(defn- node-vals [owner node-names]
  (map (comp #(.-value %) (partial om/get-node owner)) node-names))

(defn- build-form-field
  [{:keys [name type]}] 
  (html [:span 
    [:label name]
    [:input {:type type :ref name}]]))

(defn- simple-form 
  "Returns a generic hideable form component that can update app-state.
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
               [:div {:class (hide-if hidden)}
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

(def login-form 
  (simple-form "Login" [{:name "email" :type "text"}]
               :current-user (fn [[email] current-user] 
                               (:id (first (filter #(= (:email %) email)
                                             (vals (:players @app-state))))))))

(def new-player-form
  (simple-form "Create account" [{:name "Name" :type "text"}
                                 {:name "Email" :type "text"}
                                 {:name "Minimum game threshold" :type "number"}]
               :players (fn [[pname email threshold] existing] 
                          (add-player (fresh-player pname email threshold) existing))))

(def new-game-form 
  (simple-form "New game" [{:name "Date" :type "text"}]
               :games (fn [[date] games] (add-game date games))))

(defn- player-list
  [app owner]
  (om/component
    (html [:div 
           [:h3 "Registered Players"]
           [:div (map render-player (vals (:players app)))]])))

(defn- current-user [app owner]
  (om/component
    (html 
      (if-let [user (get (:players app) (:current-user app))]
        [:div [:span {:class "current-user"} (:name user)]]
        [:div [:span {:class "no-current-user"}]]))))

(defn render-app
  [app owner]
  (om/component
    (html [:div 
           (om/build current-user app)
           (om/build new-player-form app {:init-state {:hidden true}})
           (om/build new-game-form app {:init-state {:hidden true}})
           (om/build login-form app {:init-state {:hidden true}})
           (om/build game-list app)
           (om/build player-list app)])))
