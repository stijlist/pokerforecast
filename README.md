# pokerforecast

Who says they're coming to poker night? How likely is it, given how often they
flake? pokerforecast has the answer ;)

## Overview

The premise of pokerforecast is that someone's history of rsvp'ing to an event
and subsequently attending or not attending is a predictor of their likelihood
of attending a future event that they've rsvp'd to. Given that premise, we
track each potential attendee of poker night (all users of the site,
presumably) and divide their total attendances by their total rsvp's.  This
ratio is called their "attendance rate." Given two users' attendance rates, we
estimate the likelihood of a game happening which they both attend as the
product of their attendance rates. 

If we know the number of users required to attend an event for it to be
successful (the "threshold" for that event), we can calculate the likelihood of
an event happening by finding all combinations of potential attendees that are
greater than or equal to that threshold, and calculating the likelihood of that
particular game occurring using the product of those particular users'
attendance rates. 

We can determine the threshold for a game happening by asking each user what
their individual threshold is, and for each potential combination of users,
setting the threshold for that combination to be the max of those users'
thresholds (i.e. the most stringent requirement for every one of the users in
that potential game to participate). 

Finally, if we come up with every possible combination of users that might show
up and calculate the likelihood of each of those combinations, we can use the
maximum likelihood from that set as the best chance that poker will happen that
night. That's our poker forecast.

In code:

```clojure
(defn- attendance-rate [{:keys [attended rsvpd]}]
  (if (= 0 rsvpd) nil (/ attended rsvpd)))

(defn- game-likelihood [attendees]
  (->> attendees
       (map attendance-rate)
       (filter (comp not nil?))
       (reduce *)))

(defn maximum-game-likelihood [attending]
  (->> (powerset attending)
       (filter enough-players)
       (map game-likelihood)
       (apply max)
       (default 0)))
```

## Setup

First-time Clojurescript developers, add the following to your bash .profile:

    export LEIN_FAST_TRAMPOLINE=y
    alias cljsbuild="lein trampoline cljsbuild $@"

To avoid compiling ClojureScript for each build, AOT Clojurescript locally in your project with the following:

    ./scripts/compile_cljsc

Subsequent dev builds can use:

    lein cljsbuild auto dev

To start a Node REPL (requires rlwrap):

    ./scripts/repl

To get source map support in the Node REPL:

    lein npm install

Clean project specific out:

    lein clean
     
Optimized builds:

    lein cljsbuild once release     

For more info on Cljs compilation, read [Waitin'](http://swannodette.github.io/2014/12/22/waitin/).

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
