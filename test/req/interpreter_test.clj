(ns req.interpreter-test
  (:use midje.sweet)
  (:use [req.core])
  )

;; queues

(fact "new-queue will populate the queue with the seq it's handed"
  (count (new-queue [1 2 3 4])) => 4
  (peek (new-queue '(1 2 3 4))) => 1
  (new-queue [:a :b :a :b]) => [:a :b :a :b]
  )

(fact "new-queue works with no argument"
  (count (new-queue)) => 0)

(fact "conjing to a queue adds at the right"
  (rest (conj (new-queue [1 2 3 4]) 99)) => [2 3 4 99]
  )

(fact "popping from a queue removes from the left"
  (pop (new-queue [1 2 3 4])) => [2 3 4]
  )

(fact "peeking into a queue reads the item at the left"
  (peek (new-queue [1 2 3 4])) => 1
  )

;; helpers for managing items being pushed by the interpreter

(fact "append-this-to-queue appends an item or concats a seq to a seq"
  (let [my-q (new-queue [1 2 3])
        nested-q (new-queue [1 [2 3]])]
    (append-this-to-queue my-q 4) => [1 2 3 4]
    (append-this-to-queue my-q '(4 5)) => [1 2 3 4 5]
    (append-this-to-queue my-q false) => [1 2 3 false]
    (append-this-to-queue (new-queue) false) => [false]
    (append-this-to-queue nested-q [false [true]]) => [1 [2 3] false [true]]
    (append-this-to-queue nested-q {:a 8}) => [1 [2 3] {:a 8}]
    (append-this-to-queue nested-q #{1 2 3}) => [1 [2 3] #{1 2 3}]
  ))

(fact "append-this-to-queue works on a queue as expected"
  (peek (append-this-to-queue (new-queue [1 2 3]) 9)) => 1
  (peek (append-this-to-queue (new-queue [1 2 3]) [4 5 6])) => 1
  )

(fact "queue-from-items uses append-this to pile stuff onto a new queue"
  (queue-from-items [1 2] 3 [4 5]) => [1 2 3 4 5]
  (peek (queue-from-items [1 2] 3 [4 5])) => 1
  (queue-from-items [1 2] 3 [4 [5]]) => [1 2 3 4 [5]]
  (queue-from-items [1 2] '(3 4 5) false {:a 9} #{99 999}) =>
    [1 2 3 4 5 false {:a 9} #{99 999}]
  )

;; interpreter

(fact "calling req-with with a vector of items puts those onto the queue"
  (:queue (req-with [2 5 8])) => (just [2 5 8])
  (:queue (req-with [2 [3] false])) => (just [2 [3] false])
  )


;; step: literals

(fact "calling step on an interpreter containing only literals will cycle them"
  (:queue (step (req-with [false 1.2 3]))) => [1.2 3 false]
  )

(fact "the req-steps function produces a sequence of future steps"
  (let [literal (req-with [false 1.2 3])]
    (:queue (nth-step literal 1)) => [1.2 3 false]
    (:queue (nth-step literal 2)) => [3 false 1.2]
  ))