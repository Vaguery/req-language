(ns req.imperative-test
  (:use midje.sweet)
  (:use [req.core]
        [req.interpreter])
  )

;; step: imperatives

; (fact "when :dup is executed, the top item on the queue is doubled and sent to the tail"
;   (:queue (step (req-with [:dup 1.2 3]))) => (just [1.2 3 1.2])
;   (count (:queue (step (req-with [:dup])))) => 0
;   )

; (fact "when :pop is executed, the top item on the queue is thrown away"
;   (:queue (step (req-with [:pop 1 2 3 4 5]))) => (just [2 3 4 5])
;   (count (:queue (step (req-with [:pop 2])))) => 0
;   (count (:queue (step (req-with [:pop])))) => 0
;   )

; (fact "when :swap is executed, the top two items on the queue are switched (and sent to the end)"
;   (:queue (step (req-with [:swap 1 2 3 4 5]))) => (just [3 4 5 2 1])
;   (:queue (step (req-with [:swap 1 2]))) => (just [2 1])
;   (:queue (step (req-with [:swap 1]))) => (just [1])
;   (:queue (step (req-with [:swap]))) => (just [])
;   )

; (fact "when :archive is executed, the entire queue is duplicated at its own tail"
;   (:queue (step (req-with [:archive 1 2 3 4 5]))) => (just [1 2 3 4 5 1 2 3 4 5])
;   (:queue (step (req-with [:archive]))) => (just [])
;   )

; (fact "when :reverse is executed, the entire queue is flipped head-to-tail"
;   (:queue (step (req-with [:reverse 1 2 3 4 5]))) => (just [5 4 3 2 1])
;   (:queue (step (req-with [:reverse]))) => (just [])
;   )

; (fact "when :flush is executed, the entire queue is emptied"
;   (:queue (step (req-with [:flush 1 2 3 4 5]))) => (just [])
;   (:queue (step (req-with [:flush]))) => (just [])
;   )

; (fact "when :next is executed, the top item is sent to the tail"
;   (:queue (step (req-with [:next 1 2 3 4 5]))) => (just [2 3 4 5 1])
;   (:queue (step (req-with [:next 1]))) => (just [1])
;   (:queue (step (req-with [:next]))) => (just [])
;   )

; (fact "when :prev is executed, the tail item is sent to the head"
;   (:queue (step (req-with [:prev 1 2 3 4 5]))) => (just [5 1 2 3 4])
;   (:queue (step (req-with [:prev 1]))) => (just [1])
;   (:queue (step (req-with [:prev]))) => (just [])
;   )

