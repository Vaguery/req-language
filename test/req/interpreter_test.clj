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

(fact "calling make-interpreter with a vector of items puts those onto the queue"
  (:queue (make-interpreter [2 5 8])) => (just [2 5 8])
  (:queue (make-interpreter [2 [3] false])) => (just [2 [3] false])
  )


;; step: literals

(fact "calling step on an interpreter containing only literals will cycle them"
  (:queue (step (make-interpreter [false 1.2 3]))) => [1.2 3 false]
  )

(fact "the req-steps function produces a sequence of future steps"
  (let [literal (make-interpreter [false 1.2 3])]
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
  (readable-queue (step (make-interpreter [p false]))) => ["false", "«+»"]
  (readable-queue (step (make-interpreter [p]))) => ["«+»"])


(fact "a Qlosure will consume the nearest argument and produce an intermediate"
  (readable-queue (step (make-interpreter [p 1 2 4 8]))) => ["2" "4" "8" "«1+⦿»"]
  (:queue (step (step (make-interpreter [p 1 2 4 8])))) => [4 8 3]
  (:queue (step (step (step (make-interpreter [p 1 2 4 8]))))) => [8 3 4])


(fact "two Qlosure items will consume arguments according to the queue dynamics"
  (let [two-ps (make-interpreter [p 1 p 2 4 8])]
    (str (last (:queue (step two-ps)))) => "«1+⦿»"
    (readable-queue (nth-step two-ps 1)) =>  ["«+»" "2" "4" "8" "«1+⦿»"]
    (readable-queue (nth-step two-ps 2)) =>  ["4" "8" "«1+⦿»" "«2+⦿»"]
    (readable-queue (nth-step two-ps 3)) =>  ["«2+⦿»" "8" "5"]
    (readable-queue (nth-step two-ps 4)) =>  ["5" "10"]))


(fact "Qlosure items will still skip (and requeue) unwanted items as needed"
  (let [two-ps (make-interpreter [p 1 p false 4 8])]
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
  (let [two-ps (make-interpreter [p p 1 2 4 8])]
    (readable-queue (step two-ps)) => ["2" "4" "8" "«+»" "«1+⦿»"]
    (readable-queue (step (step two-ps))) => ["«1+⦿»" "4" "8" "«2+⦿»"]
    (readable-queue (step (step (step two-ps)))) => ["8" "«2+⦿»" "5"]
    (readable-queue (step (step (step (step two-ps))))) => ["5" "10"]))


;; multiple result values can be returned from interactions


(def «3x»
  "a 1-ary Qlosure which produces three copies of its (any-type) argument"
  (make-unary-qlosure "3x" :req.items/thing
  (partial #(list % % %))))


(fact "multiple return values (returned as a Clojure list) are pushed onto the queue"
  (let [tripler (make-interpreter [«3x» -1 «3x» «3x» false -4 8])]
    (readable-queue (nth-step tripler 1)) =>
      ["«3x»" "«3x»" "false" "-4" "8" "-1" "-1" "-1"]
    (readable-queue (nth-step tripler 2)) =>
      ["false" "-4" "8" "-1" "-1" "-1" "«3x»" "«3x»" "«3x»"]
    (readable-queue (nth-step tripler 3)) =>
      ["«3x»" "«3x»" "-4" "8" "-1" "-1" "-1" "false" "false" "false"]
    (readable-queue (nth-step tripler 4)) =>
      ["-4" "8" "-1" "-1" "-1" "false" "false" "false" "«3x»" "«3x»" "«3x»"]
    (readable-queue (nth-step tripler 5)) =>
      ["«3x»" "«3x»" "8" "-1" "-1" "-1" "false" "false" "false" "-4" "-4" "-4"]
      ;; and so on...
    (readable-queue (nth-step tripler 32)) =>
      ["-1" "-1" "-1" "-1" "-1" "-1" "-1" "-1" "-1" "false" "false" "false" "false" "false" "false" "false" "false" "false" "-4" "-4" "-4" "-4" "-4" "-4" "-4" "-4" "-4" "8" "8" "8" "8" "8" "8" "8" "8" "8" "«3x»" "«3x»" "«3x»"]))


;; collections can be returned from interactions


(def «3xVec»
  "1-ary Qlosure which produces a Clojure vector containing 3x copies of its :thing arg"
  (make-unary-qlosure "3xVec" :req.items/thing
  (partial #(list (vector % % %)))))


(fact "for a collection result to be handled correctly by the ReQ interpreter,
  the :transition function should wrap it in a list"
  (let [tripler (make-interpreter [«3xVec» -1  false «3xVec» -4 «3xVec» 8])]
    (readable-queue (step tripler)) =>
      ["false" "«3xVec»" "-4" "«3xVec»" "8" "[-1 -1 -1]"]
    (readable-queue (nth-step tripler 2)) =>  
      ["-4" "«3xVec»" "8" "[-1 -1 -1]" "[false false false]"]
    (readable-queue (nth-step tripler 3)) =>  
      ["8" "[-1 -1 -1]" "[false false false]" "[-4 -4 -4]"]))


;;
;; Nullary items
;;
;; A Nullary is like a Qlosure, except that it has no arguments and thus no :wants. 
;; Whenever a Nullary is executed, it produces its result


(fact "a Nullary has a token it shows when printed, like a Qlosure"
  (str (make-nullary "beep" nil)) => "«beep»")


(def silly
  "a Nullary which produces the number 29 when executed"
  (make-nullary "29" (constantly 29))) ;; definitely NOT an exciting example!


(fact "a Nullary produces its result (which is pushed) when it is in the hot seat"
  (readable-queue (make-interpreter [silly false])) => ["«29»" "false"]
  (readable-queue (step (make-interpreter [silly false]))) => ["false" "29"])


;; Nullary "seeds"


(def eights
  "a Nullary which produces itself and a number 8 every time it's executed"
  (make-seed "8s" 8))


(fact "a Nullary produces its result (which is pushed) when it is in the hot seat"
  (let [eighty (make-interpreter [eights false])]
    (readable-queue eighty) => ["«8s»" "false"]
    (readable-queue (step eighty)) => ["false" "«8s»" "8"]
    (readable-queue (nth-step eighty 2)) => ["«8s»" "8" "false"]
    (readable-queue (nth-step eighty 3)) => ["8" "false" "«8s»" "8"]))


;; Nullary "timers"


(def c
  "a simple timer that counts from 2 to 4, then disappears and becomes 4"
  (make-timer 2 4))


(fact "a Nullary timer advances at every execution, and eventually becomes its end value"
  (readable-queue (make-interpreter [c false])) => ["«timer:2-4»" "false"]
  (readable-queue (step (make-interpreter [c false]))) => ["false" "«timer:3-4»"]
  (readable-queue (step (step (make-interpreter [c false])))) => ["«timer:3-4»" "false"]
  (readable-queue (step (step (step (make-interpreter [c false]))))) => ["false" "4"])


(def c
  "a Nullary timer with a payload: it emits :foo on every execution"
  (make-timer 2 11 :foo))


(fact "a Nullary timer with a payload emits its payload every cycle, then dissolves"
  (let [timey (make-interpreter [c false])]
    (readable-queue (nth-step timey 0)) => ["«timer:2-11»" "false"]
    (readable-queue (nth-step timey 1)) => ["false" "«timer:3-11»" ":foo"]
    (readable-queue (nth-step timey 2)) => ["«timer:3-11»" ":foo" "false"]
    (readable-queue (nth-step timey 3)) => [":foo" "false" "«timer:4-11»" ":foo"]
    (readable-queue (nth-step timey 4)) => ["false" "«timer:4-11»" ":foo" ":foo"]
    (readable-queue (nth-step timey 5)) => ["«timer:4-11»" ":foo" ":foo" "false"]
    (readable-queue (nth-step timey 16)) =>
      [":foo" ":foo" ":foo" "false" "«timer:7-11»" ":foo" ":foo"]
    (readable-queue (nth-step timey 32)) =>
      [":foo" ":foo" "false" "«timer:9-11»" ":foo" ":foo" ":foo" ":foo" ":foo"]
    (readable-queue (nth-step timey 210)) =>
      [":foo" ":foo" ":foo" "false" "11" ":foo" ":foo" ":foo" ":foo" ":foo"]))


;; Nullary loopers


(def jenny
  "a Nullary looper that eternally cycles through its vector contents, emitting the top item each step"
  (make-looper [9 0 3 5 7 6 8]))


(fact "a Nullary looper cycles its (sequence) payload, emitting the top item each step"
  (let [her-number (make-interpreter [jenny false])]
    (readable-queue (nth-step her-number 0 )) => ["«[9 0 3 5 7 6 8]»" "false"]
    (readable-queue (nth-step her-number 1 )) => ["false" "«[0 3 5 7 6 8 9]»" "9"]
    ;; ... much later ...
    (readable-queue (nth-step her-number 1202 )) =>
      ["8" "6" "7" "5" "3" "0" "9" "8" "6" "7" "5" "3" "0" "9" "8" "6" "7" "5" "3" "0" "9" "false" "«[8 9 0 3 5 7 6]»" "6" "7" "5" "3" "0" "9" "8" "6" "7" "5" "3" "0" "9" "8" "6" "7" "5" "3" "0" "9" "8" "6" "7" "5" "3" "0" "9"]))
