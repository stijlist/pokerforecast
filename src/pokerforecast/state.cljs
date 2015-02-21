(ns pokerforecast.state)

(def app-state (atom {:games [{:date "Monday, January 18"
                                :attending [1 2]}
                               {:date "Tuesday, January 19"
                                :attending [3 4 1]}]
                      :players {1 {:name "James"
                                   :id 1
                                   :rsvpd 3
                                   :email "yolo@swag.com"
                                   :attended 2
                                   :threshold 3}
                                2 {:name "Nick"
                                   :id 2
                                   :rsvpd 1
                                   :email "foo@bar.com"
                                   :attended 1
                                   :threshold 4}
                                3 {:name "Bert"
                                   :id 3
                                   :email "blah@blah.com"
                                   :rsvpd 2
                                   :attended 1
                                   :threshold 2}
                                4 {:name "Max"
                                   :id 4
                                   :email "some@thing.com"
                                   :rsvpd 1
                                   :attended 1
                                   :threshold 3}}
                      :current-user nil}))
