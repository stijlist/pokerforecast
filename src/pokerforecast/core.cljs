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

(defn render-game-list
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

(defn render-new-player-form
  [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [hidden] :as state}]
      (dom/div nil
        (dom/button #js {:onClick #(if hidden 
                                     (om/set-state! owner :hidden false))} 
                         "Create account")
        (dom/div #js {:className (hide-if hidden)}
                 (dom/form #js {:onSubmit (fn [e] 
                                            (.preventDefault e)
                                            (om/transact! app :players
                                              (->> (node-vals owner "name" "email" "threshold")
                                                (apply fresh-player)
                                                (partial add-player))))}
                   (dom/label nil "Name")
                   (dom/input #js {:type "text" :ref "name"})
                   (dom/label nil "Email")
                   (dom/input #js {:type "text" :ref "email"})
                   (dom/label nil "Minimum game size")
                   (dom/input #js {:type "number" :ref "threshold"})
                   (dom/input #js {:type "submit" })))))))

(defn render-player-list
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
      (om/build render-new-player-form app {:init-state {:hidden true}})
      (om/build render-game-list app)
      (om/build render-player-list app))))

(om/root 
  render-app
  app-state
  {:target (. js/document getElementById "app")})
