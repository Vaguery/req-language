(ns req.interpreter-test
  (:use midje.sweet)
  (:use [req.core]
        [req.interpreter]
        [req.items])
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


;; The interpreter cycle:
;;   pop an item
;;   if it's an interpreter instruction, do it
;;   if it interacts with anything on the queue,
;;     - pop everything it skips
;;     - create the interaction result(s)
;;     - push the popped things to the tail of the queue
;;     - push the result(s) onto the tail of the queue
;;     - push any immortal arguments to the tail of the queue
;;   if it doesn't interact, push it to the tail of the queue


;; Qlosures on the queue


(def p
  "polymorphic 2-ary Qlosure implementing both ::num addition and ::vec concatenation"
  (make-qlosure
    "+"                             
    :wants
       {:num1 number?, :vec1 vector?}     
    :transitions
      {:num1                              
        (fn [item]
          (make-qlosure
            (str item "+⦿")
            :wants {:num2 number?}
            :transitions {:num2 (partial + item)}))
       :vec1
        (fn [item]
          (make-qlosure
            (str item "+⦿")
            :wants {:vec2 vector?}
            :transitions {:vec2 (partial into item)}))}))


(fact "a ReQ interpreter with a Qlosure with no targets just keeps it"
  (readable-queue (step (req-with [p false]))) => ["false", "«+»"]
  (readable-queue (step (req-with [p]))) => ["«+»"])


(fact "a Qlosure will consume the nearest argument and produce an intermediate"
  (readable-queue (step (req-with [p 1 2 4 8]))) => ["2" "4" "8" "«1+⦿»"]
  (:queue (step (step (req-with [p 1 2 4 8])))) => [4 8 3]
  (:queue (step (step (step (req-with [p 1 2 4 8]))))) => [8 3 4])


(fact "two Qlosure items will consume arguments according to the queue dynamics"
  (let [two-ps (req-with [p 1 p 2 4 8])]
    (str (last (:queue (step two-ps)))) => "«1+⦿»"
    (readable-queue (nth-step two-ps 1)) =>  ["«+»" "2" "4" "8" "«1+⦿»"]
    (readable-queue (nth-step two-ps 2)) =>  ["4" "8" "«1+⦿»" "«2+⦿»"]
    (readable-queue (nth-step two-ps 3)) =>  ["«2+⦿»" "8" "5"]
    (readable-queue (nth-step two-ps 4)) =>  ["5" "10"]))


(fact "Qlosure items will still skip (and requeue) unwanted items as needed"
  (let [two-ps (req-with [p 1 p false 4 8])]
    (readable-queue (nth-step two-ps 1)) =>  ["«+»" "false" "4" "8" "«1+⦿»"]
    (readable-queue (nth-step two-ps 2)) =>  ["8" "«1+⦿»" "false" "«4+⦿»"]
    (readable-queue (nth-step two-ps 3)) =>  ["false" "«4+⦿»" "9"]
    (readable-queue (nth-step two-ps 4)) =>  ["«4+⦿»" "9" "false"]
    (readable-queue (nth-step two-ps 5)) =>  ["false" "13"]
  ))


;; the following is CURRENTLY correct for the final ReQ language spec...
;; but in the long term, Qlosures should be able to act as viable arguments
;; for other Qlosures, if their :type matches
(fact "stepping through with two Qlosures which might interact (some day) on the interpreter"
  (let [two-ps (req-with [p p 1 2 4 8])]
    (readable-queue (step two-ps)) => ["2" "4" "8" "«+»" "«1+⦿»"]
    (readable-queue (step (step two-ps))) => ["«1+⦿»" "4" "8" "«2+⦿»"]
    (readable-queue (step (step (step two-ps)))) => ["8" "«2+⦿»" "5"]
    (readable-queue (step (step (step (step two-ps))))) => ["5" "10"]))
