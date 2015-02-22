(ns pokerforecast.state)

(def app-state (atom {:root 
                      {:games [{:date "Monday, January 18"
                                :attending [1 2]}
                                {:date "Tuesday, January 19"
                                 :attending [3 4 1]}]
                        :players {1 {:name "James"
                                     :rsvpd 3
                                     :email "yolo@swag.com"
                                     :password "bulletsforever"
                                     :attended 2
                                     :threshold 3}
                                  2 {:name "Nick"
                                     :rsvpd 1
                                     :email "foo@bar.com"
                                     :password "calligrapher"
                                     :attended 1
                                     :threshold 4}
                                  3 {:name "Bert"
                                     :email "blah@blah.com"
                                     :password "enthusiastic"
                                     :rsvpd 2
                                     :attended 1
                                     :threshold 2}
                                  4 {:name "Max"
                                     :email "some@thing.com"
                                     :password "cowpig"
                                     :rsvpd 1
                                     :attended 1
                                     :threshold 3}}
                        :current-user nil}}))
