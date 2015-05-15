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

## Motivation and future plans

This app is being written as much for my own edification w.r.t. ClojureScript
in general and designing in the browser as it is to scratch an itch and be a
little funny. As it stands, I've spent an inordinate amount of time nibbling
around the edges of CSS layout and higher-order-components in Om, and not much
time building out the app beyond calculating the likelihood you see above. 

One possible direction for the app might be helping the user make better
decisions as they're planning a given poker night before they even post it;
giving them the option of asking hypotheticals ("What's the poker forecast if I
invited Bill and James?") and acting based on the results. 

Another feature I'd like is displaying a line graph of how the likelihood
changes over time as we get more information, and noting what the events are
that cause each change. For example, if someone RSVPs for a game and the
likelihood goes up from 67% to 80%, it would be great to show on a graph a
marker for that event and the subsequent spike in likelihood. The same would be
true if someone un-RSVPs, or we gain more information about them after they
flake for a different game.

The implementations for these two features are interesting. The first,
answering hypotheticals in the planning stage, might benefit from incremental
calculation of the powerset of all players: the addition or removal of a given
player would trigger an O(n) update as we either generate new subsets with the
player added, or remove all subsets containing the player. The second,
displaying the line graph of change over time, might benefit from incremental
calculation of the state of the app. This could be implemented as some kind of
reducing function that takes the current state of the app as a memo, a new
piece of information to be reduced, and returns the new state of the app. Given
a log of events, then, and an empty app-state, we can graph the change over
time of our forecasts. These events could be windowed or paginated in some way
to prevent the computation time of our graphs from growing without bound.

Finally, it is probably obvious to you that nothing about this app is specific
to poker; it could (and probably should!) be adapted to handle karaoke nights
and office seminars equally as well as it handles Texas hold 'em. Right now,
the only thing the app takes advantage of is the idea that any n-handed poker
game is equivalent to any other n-handed poker game in sufficiency, while
someone's attendance at an office seminar or karaoke night could be predicated
on a complex, interdependent list of criteria. I'll hold off on trying to model
this until I've finished or abandoned the two features above, but it's an 
interesting challenge and a potentially make-or-break feature for the app to
obtain broader appeal.

## Setup

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

For more info on cljs compilation, read [Waitin'](http://swannodette.github.io/2014/12/22/waitin/).

## License

Copyright © 2014 Bert Muthalaly

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
