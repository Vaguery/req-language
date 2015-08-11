(ns req.core-test
  (:use midje.sweet)
  (:use [req.core]))

;; queues

(fact "empty-queue will return an empty queue (duh)"
  (count (empty-queue)) => 0)

(fact "new-queue will populate the queue with the seq it's handed"
  (count (new-queue [1 2 3 4])) => 4
  (peek (new-queue [1 2 3 4])) => 1
  (new-queue [:a :b :a :b]) => (just [:a :b :a :b]))

(fact "conjing to a queue adds at the right"
  (rest (conj (new-queue [1 2 3 4]) 99)) => (just [2 3 4 99]))

(fact "popping from a queue removes from the left"
  (pop (new-queue [1 2 3 4])) => (just [2 3 4]))