(ns pokerforecast.view
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [clojure.string :as string]
            [pokerforecast.core :as forecast]
            [pokerforecast.form :as form :refer [button-to form]]))

(defn- inspect [thing] (println thing) thing)

(defn- classes [& cs] (apply str (interpose " " cs)))

(defn- as-percentage [n] (.toFixed (* n 100)))

(defn- join-players [players ids]
  (map (partial get players) ids))

(defn- fresh-player [new-name email password threshold]
  {:attended 0 :rsvpd 0 :name new-name :threshold threshold})

(defn- add-player [player existing]
  (let [next-id (inc (apply max (keys existing)))]
    (assoc existing next-id player)))

(defn- add-game [date existing]
  (conj existing {:date date :players [] :hidden true}))

(defn- render-player [attendee]
  (html [:li 
    [:span {:class "attendee"} (:name attendee)] 
    (if (> (:rsvpd attendee) 0)
      [:span {:class "flake-rate"}
       (as-percentage (forecast/flake-rate attendee))]
      [:span {:class "no-flake-rate"}])]))

(defn- game-view [{:keys [game players current-user]} owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [hidden]}]
      (let [attending (join-players players (:attending game))
            rsvpd (some #{current-user} (:attending game))]
        (html [:li {:class (if rsvpd "rsvpd")}
               [:span {:class "date"} (:date game)]
               [:span {:class "likelihood"}
                (as-percentage (forecast/maximum-game-likelihood attending))]
               [:span {:class "confidence"} 
                (as-percentage (forecast/likelihood-confidence attending))]
               [:span {:class "attending-count"} (count attending)]
               (button-to "Show attending" #(om/update-state! owner :hidden not))
               (if rsvpd
                 (button-to "Change RSVP" 
                   #(om/transact! game :attending 
                                  (partial remove (partial = current-user))))
                 (button-to "RSVP" {:disabled (not current-user)}
                   #(om/transact! game :attending 
                                  (fn [g] (conj g current-user)))))
               [:ul {:class (classes "attendees" (if hidden "hide" ""))} 
                (map render-player attending)]])))))

(defn- game-list [app owner]
  (om/component
    (let [cursors (select-keys app [:players :current-user :game])]
      (html 
        [:ul 
         (om/build-all game-view 
           (map (partial assoc cursors :game) (:games app))
           {:init-state {:hidden true}})]))))

(defn- validate-date [date]
  (let [[mm dd yyyy] (string/split date "/")]
    (and (= 2 (count mm)) (= 2 (count dd)) (= 4 (count yyyy)))))

(defn- player-list [app owner]
  (om/component
    (html [:div 
           [:h3 "Registered Players"]
           [:div (map render-player (vals (:players app)))]])))

(defn date-component [app owner]
  (reify
    om/IInitState
    (init-state [_] {:text ""})
    om/IRenderState
    (render-state [this state]
      (html 
        [:div 
         [:label "Date"]
         [:input {:type "text" 
                  :value (:text state)
                  :onChange 
                  (fn [e] 
                    (om/set-state! owner :text (.. e -target -value)))}]]))))

(def date-field
  (specify! date-component
    form/Field
    (value [this data owner] (om/get-state owner :text))
    (validation-error? [this data owner]
      (if-not 
        (validate-date (inspect (om/get-state owner :text))) 
        "Please enter a date in jm/dd/yyyy format"))
    (update-state [this data owner value] 
      (om/transact! data :games (partial add-game value)))))

(defn app [data owner]
  (om/component
    (html 
      [:div 
       (om/build (form "New game" date-field) data)
       (om/build game-list data)
       (om/build player-list data)])))
