(ns pokerforecast.core
  (:require [clojure.browser.repl :as repl]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(enable-console-print!)

(defn inspect [thing] (println thing) thing)

(def app-state (atom {:games [{:date "Monday, January 18"
                                :attending [1 2]}
                               {:date "Tuesday, January 19"
                                :attending [3 4 1]}]
                      :players {1 {:name "James"
                                   :rsvpd 3
                                   :email "yolo@swag.com"
                                   :attended 2
                                   :threshold 3}
                                2 {:name "Nick"
                                   :rsvpd 1
                                   :email "foo@bar.com"
                                   :attended 1
                                   :threshold 4}
                                3 {:name "Bert"
                                   :email "blah@blah.com"
                                   :rsvpd 2
                                   :attended 1
                                   :threshold 2}
                                4 {:name "Max"
                                   :email "some@thing.com"
                                   :rsvpd 1
                                   :attended 1
                                   :threshold 3}}
                      :logged-in-user nil}))

;; FORECASTING

(defn attendance-rate
  [{:keys [attended rsvpd]}]
  (if (= 0 rsvpd) nil (/ attended rsvpd)))

(defn powerset
  [coll]
  (if-not (seq coll) 
    [[]]
    (let [rest-powerset (powerset (rest coll))]
      (concat (map #(conj % (first coll)) rest-powerset) rest-powerset))))

(defn game-likelihood
  [attendees]
  (->> attendees
       (map attendance-rate) ; what do we do with unknown attendance rates?
       (filter (comp not nil?))
       (reduce *)))

(defn enough-players [attendees]
  (>= 
    (count attendees)
    (apply max (map :threshold attendees))))

(defn default [d input]
  (if (nil? input) d input))

(defn maximum-game-likelihood 
  [{:keys [attending]}]
  (->> (powerset attending) ; all combinations of folks that might show up
       (filter enough-players)
       (map game-likelihood)
       (apply max)
       (default 0)))

(defn as-percentage [n] 
  (.toFixed (* n 100)))

(defn flake-rate [attendee]
  (- 1 (attendance-rate attendee)))

;; PRESENTATION

(defn hide-if [hidden?]
  (if hidden? "hide" ""))

(defn classes [& cs]
  (apply str (interpose " " cs)))

(defn render-date
  [game]
  (html [:span {:className "date"}
    (:date game)]))

(defn render-likelihood
  [game]
  (html [:span {:class "likelihood"}
         (as-percentage (maximum-game-likelihood game))]))

(defn render-attending-count
  [game]
  (html [:span {:className "attending"}
    (count (:attending game))]))

(defn render-player
  [attendee]
  (html [:li 
    [:span {:className "attendee"} (:name attendee)] 
    (if (> (:rsvpd attendee) 0)
      [:span {:className "flake-rate"}
       (as-percentage (flake-rate attendee))]
      [:span {:className "no-flake-rate"}])]))

(defn render-attendees
  [game hidden]
  (html [:ul {:className (classes "attendees" (hide-if hidden))}
    (map render-player (:attending game))]))

(defn game-view 
  [game owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [hidden]}]
      (html [:li 
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

(defn game-list
  [{:keys [players games] :as app} owner]
  (reify
    om/IRenderState
    (render-state [this state]
      (html 
        [:ul (om/build-all game-view 
                      (->> games (with-players players))
                      {:init-state {:hidden true}})]))))

(defn node-vals [owner & node-names]
  (map (comp #(.-value %) (partial om/get-node owner)) node-names))

(defn build-field
  [{:keys [field-name field-type]}] 
  (html [:span 
    [:label field-name]
    [:input {:type field-type :ref field-name}]]))

(defn simple-form 
  "Returns a generic hideable form component.
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
               [:button {:onClick #(if hidden 
                                        (om/set-state! owner :hidden false))}
                form-name]
               [:div {:className (hide-if hidden)}
                [:form 
                 {:onSubmit 
                      (fn [e] 
                        (.preventDefault e)
                        (om/transact! app update-path 
                                      (->>
                                        (apply node-vals owner (map :field-name fields))
                                        (partial update-fn)))
                        (om/set-state! owner :hidden true))}
                 (for [field fields] 
                   (build-field field))
                 [:input {:type "submit"}]]]])))))

(def login-form 
  (simple-form "Login" [{:field-name "email" :field-type "text"}]
               :logged-in-user (fn [[email] current-user] 
                                 (first (filter #(= (:email %) email)
                                                (vals (:players @app-state)))))))

(defn fresh-player [new-name email threshold]
  (assoc {:attended 0 :rsvpd 0} :name new-name :threshold threshold))

(defn add-player [player existing]
  (let [next-id (inc (apply max (keys existing)))]
    (assoc existing next-id player)))

(def new-player-form
  (simple-form "Create account" [{:field-name "Name" :field-type "text"}
                                 {:field-name "Email" :field-type "text"}
                                 {:field-name "Minimum game threshold" :field-type "number"}]
               :players (fn [[pname email threshold] existing] 
                          (add-player (fresh-player pname email threshold) existing))))

(defn add-game
  [date existing]
  (conj existing {:date date :players [] :hidden true}))

(def new-game-form 
  (simple-form "New game" [{:field-name "Date" :field-type "text"}]
               :games (fn [[date] games] (add-game date games))))

(defn player-list
  [app owner]
  (om/component
    (html [:div 
           [:h3 "Registered Players"]
           [:div (map render-player (vals (:players app)))]])))

(defn render-app
  [app owner]
  (om/component
    (html [:div 
           (om/build new-player-form app {:init-state {:hidden true}})
           (om/build new-game-form app {:init-state {:hidden true}})
           (om/build login-form app {:init-state {:hidden true}})
           (om/build game-list app)
           (om/build player-list app)])))

(om/root 
  render-app
  app-state
  {:target (. js/document getElementById "app")})
