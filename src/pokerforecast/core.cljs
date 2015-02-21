(ns pokerforecast.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [clojure.browser.repl :as repl]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [cljs.core.async :refer [put! chan <!]]))

(enable-console-print!)

(defn inspect [thing] (println thing) thing)

(def app-state (atom {:games [{:date "Monday, January 18"
                                :attending [1 2]
                                :hidden true}
                               {:date "Tuesday, January 19"
                                :attending [3 4 1]
                                :hidden true}]
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

(defn hide-if [hidden?]
  (if hidden? "hide" ""))

(defn classes [& cs]
  (apply str (interpose " " cs)))

(defn render-date
  [game]
  (dom/span #js {:className "date"}
            (:date game)))

(defn render-likelihood
  [game]
  (dom/span #js {:className "likelihood"}
            (as-percentage (maximum-game-likelihood game))))

(defn render-attending-count
  [game]
  (dom/span #js {:className "attending"}
            (count (:attending game))))

(defn render-player
  [attendee]
  (dom/li nil 
          (dom/span #js {:className "attendee"} (:name attendee)) 
          (if (> (:rsvpd attendee) 0)
            (dom/span #js {:className "flake-rate"}
                      (as-percentage (flake-rate attendee)))
            (dom/span #js {:className "no-flake-rate"}))))

(defn render-attendees
  [game]
  (apply dom/ul #js {:className (classes "attendees" (hide-if (:hidden game)))}
         (map render-player (:attending game))))

(defn game-view 
  [game owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [toggle-chan]}]
      (dom/li nil
        (render-date game) 
        (render-likelihood game)
        (render-attending-count game)
        (dom/button #js {:onClick #(put! toggle-chan (:index game))} 
                    "Show attending")
        (render-attendees game)))))

(defn- with-indices [coll]
  (map-indexed (fn [i item] (assoc item :index i)) coll))

(defn- with-players [players games]
  (map 
    #(update-in % [:attending] (partial mapv (partial get players))) 
    games))

(defn game-list
  [{:keys [players games] :as app} owner]
  (reify
    om/IInitState
    (init-state [_]
      {:toggle-chan (chan)})

    om/IWillMount
    (will-mount [this]
      (let [toggle-chan (om/get-state owner :toggle-chan)]
        (go-loop [] 
          (om/transact! app [:games (<! toggle-chan) :hidden] not)
          (recur))))

    om/IRenderState
    (render-state [this {:keys [toggle-chan]}]
      (apply dom/ul nil 
            (om/build-all game-view 
                          (->> games with-indices (with-players players))
                          {:init-state {:toggle-chan toggle-chan}})))))

(defn fresh-player [new-name email threshold]
  (assoc {:attended 0 :rsvpd 0} :name new-name :threshold threshold))

(defn node-vals [owner & node-names]
  (map (comp #(.-value %) (partial om/get-node owner)) node-names))

(defn add-player [player existing]
  (let [next-id (inc (apply max (keys existing)))]
    (assoc existing next-id player)))

; fields is a vector of maps, containing the keys field-name and field-type, 
; which are the label of the field and the dom input type to be used, respectively
; update-fn takes a vector of values (the values in `fields`, ordered, at the 
; moment the user hits submit) and the app state at `update-path` and returns 
; the new app state at `update-path`
(defn simple-form 
  [form-name fields update-path update-fn]
  (fn [app owner]
    (reify
      om/IRenderState
      (render-state [this {:keys [hidden] :as state}]
        (dom/div nil
          (dom/button #js {:onClick #(if hidden 
                                       (om/set-state! owner :hidden false))}
                      form-name)
          (dom/div #js {:className (hide-if hidden)}
                   (apply dom/form 
                          #js {:onSubmit 
                               (fn [e] 
                                 (.preventDefault e)
                                 (om/transact! app update-path 
                                   (->>
                                     (apply node-vals owner (map :field-name fields))
                                     (partial update-fn)))
                                 (om/set-state! owner :hidden true))}
                          (conj 
                            (mapv 
                              (fn [{:keys [field-name field-type]}] 
                                (dom/span nil
                                  (dom/label nil field-name)
                                  (dom/input #js {:type field-type :ref field-name})))
                              fields)
                            (dom/input #js {:type "submit"})))))))))

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

(defn add-game
  [date existing]
  (conj existing {:date date :players [] :hidden true}))

(defn new-game-form
  [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [hidden] :as state}]
      (dom/div nil
        (dom/button #js {:onClick #(if hidden 
                                     (om/set-state! owner :hidden false))} 
                         "New game")
        (dom/div #js {:className (hide-if hidden)}
                 (dom/form #js {:onSubmit (fn [e] 
                                            (.preventDefault e)
                                            (om/transact! app :games
                                              (partial add-game
                                                (.-value (om/get-node owner "date")))))}
                   (dom/label nil "Date")
                   (dom/input #js {:type "text" :ref "date"})
                   (dom/input #js {:type "submit" })))))))

(defn player-list
  [app owner]
  (om/component
    (dom/div nil
      (dom/h3 nil "Registered Players")
      (apply dom/div nil
             (map render-player (vals (:players app)))))))

(defn render-app
  [app owner]
  (om/component
    (dom/div nil
      (om/build new-player-form app {:init-state {:hidden true}})
      (om/build new-game-form app {:init-state {:hidden true}})
      (om/build login-form app {:init-state {:hidden true}})
      (om/build game-list app)
      (om/build player-list app))))

(om/root 
  render-app
  app-state
  {:target (. js/document getElementById "app")})
