(ns req.core-test
  (:use midje.sweet)
  (:use [req.core])
  )

;; queues

(fact "new-queue will populate the queue with the seq it's handed"
  (count (new-queue [1 2 3 4])) => 4
  (peek (new-queue '(1 2 3 4))) => 1
  (new-queue [:a :b :a :b]) => (just [:a :b :a :b])
  )

(fact "conjing to a queue adds at the right"
  (rest (conj (new-queue [1 2 3 4]) 99)) => (just [2 3 4 99])
  )

(fact "popping from a queue removes from the left"
  (pop (new-queue [1 2 3 4])) => (just [2 3 4])
  )

;; interpreter

(fact "calling req-with with a vector of items puts those onto the queue"
  (:queue (req-with [2 5 8])) => (just [2 5 8])
  (:queue (req-with [2 [3] false])) => (just [2 [3] false])
  )

;; step: literals

(fact "calling step on an interpreter containing only literals will cycle them"
  (:queue (step (req-with [false 1.2 3]))) => (just [1.2 3 false])
  (:queue (step (step (req-with [false 1.2 3])))) => (just [3 false 1.2])
  )

;; step: imperatives

(fact "when :dup is executed, the top item on the queue is doubled and sent to the tail"
  (:queue (step (req-with [:dup 1.2 3]))) => (just [1.2 3 1.2])
  (count (:queue (step (req-with [:dup])))) => 0
  )

(fact "when :pop is executed, the top item on the queue is thrown away"
  (:queue (step (req-with [:pop 1 2 3 4 5]))) => (just [2 3 4 5])
  (count (:queue (step (req-with [:pop 2])))) => 0
  (count (:queue (step (req-with [:pop])))) => 0
  )

(fact "when :swap is executed, the top two items on the queue are switched (and sent to the end)"
  (:queue (step (req-with [:swap 1 2 3 4 5]))) => (just [3 4 5 2 1])
  (:queue (step (req-with [:swap 1 2]))) => (just [2 1])
  (:queue (step (req-with [:swap 1]))) => (just [1])
  (:queue (step (req-with [:swap]))) => (just [])
  )

(fact "when :archive is executed, the entire queue is duplicated at its own tail"
  (:queue (step (req-with [:archive 1 2 3 4 5]))) => (just [1 2 3 4 5 1 2 3 4 5])
  (:queue (step (req-with [:archive]))) => (just [])
  )

(fact "when :reverse is executed, the entire queue is flipped head-to-tail"
  (:queue (step (req-with [:reverse 1 2 3 4 5]))) => (just [5 4 3 2 1])
  (:queue (step (req-with [:reverse]))) => (just [])
  )

(fact "when :flush is executed, the entire queue is emptied"
  (:queue (step (req-with [:flush 1 2 3 4 5]))) => (just [])
  (:queue (step (req-with [:flush]))) => (just [])
  )

(fact "when :next is executed, the top item is sent to the tail"
  (:queue (step (req-with [:next 1 2 3 4 5]))) => (just [2 3 4 5 1])
  (:queue (step (req-with [:next 1]))) => (just [1])
  (:queue (step (req-with [:next]))) => (just [])
  )

(fact "when :prev is executed, the tail item is sent to the head"
  (:queue (step (req-with [:prev 1 2 3 4 5]))) => (just [5 1 2 3 4])
  (:queue (step (req-with [:prev 1]))) => (just [1])
  (:queue (step (req-with [:prev]))) => (just [])
  )

;; Qlosures

(fact "a Qlosure is created with a token,
    which appears in guillemets when it's printed with (str …)"
  (str (->Qlosure "+" :foo)) => "«+»"
  (str (->Qlosure "skeleton" :foo)) => "«skeleton»"
  (str (->Qlosure "9+_" :foo)) => "«9+_»"
  )

;; a Qlosure must also have a :transitions attribute, which should
;;   be a single map with map-valued values, which must contain a
;;   :want and a :transformation associated with that :want

(fact "get-wants produces the :wants component of a Qlosure's :transitions table"
  (get-wants (->Qlosure "+" {:wants {:arg1 9}})) => {:arg1 9}
  )

(fact "We can tell when (and how) an item `req-wants` another (indicating it can act as an arg)"
  (req-wants 3 4) => false
  (req-wants 3 false) => false
  (req-wants [1 2] [false :g]) => false

  (let [q (->Qlosure :foo {:wants {:int integer?, :float float?, :vec vector?}})]
    (req-wants q 4) => truthy       ;; :int
    (req-wants q 4) => :int

    (req-wants q false) => falsey   ;; false
    (req-wants q [1 2 3]) => truthy ;; :vec
    (req-wants q [1 2 3]) => :vec

    (req-wants q "nope") => falsey  ;; false
    (req-wants q 4.3) => truthy     ;; :float
    (req-wants q 4.3) => :float
    )
  )

(fact "can-interact? determines whether either item wants the other"
  (can-interact? 3 4) => false
  (can-interact? 3 false) => false
  (can-interact? [1 2] [false :g]) => false
  (let [q (->Qlosure :foo {:wants {:int integer?, :float float?, :vec vector?}})]
    (can-interact? q 4) => true
    (can-interact? 4 q) => true
    (can-interact? q q) => false
  ))

(fact "do-not-interact? determines whether either item wants the other"
  (do-not-interact? 3 4) => true
  (do-not-interact? 3 false) => true
  (do-not-interact? [1 2] [false :g]) => true
  (let [q (->Qlosure :foo {:wants {:int integer?, :float float?, :vec vector?}})]
    (do-not-interact? q 4) => false
    (do-not-interact? 4 q) => false
    (do-not-interact? q q) => true
  ))

(fact "all-interacting-items returns everything in a collection that can interact with the (first arg) ReQ item"
  (let [q (->Qlosure :foo {:wants {:int integer?, :float float?, :vec vector?}})]
    (all-interacting-items 3 [1 2 3 4 5 6]) => []
    (all-interacting-items q [1 nil -11 3.2 '(4)]) => [1 -11 3.2]
    (all-interacting-items q []) => []
    (all-interacting-items q [1 1 [1 1] 1]) => [1 1 [1 1] 1]
  ))

(fact "split-with-interaction takes a sequential collection and splits it at the first item that interacts with the actor (arg 1)"
  (fact "if there is no interaction, the first list contains everything"
    (split-with-interaction 3 [1 2 3 4 5 6]) => ['(1 2 3 4 5 6) '()])
  (let [q (->Qlosure :foo {:wants {:int integer?, :vec vector?}})]
    (split-with-interaction q [1.2 3.4 5 67]) => ['(1.2 3.4) '(5 67)]
    (split-with-interaction q [1.2 3.4 false 67]) => ['(1.2 3.4 false) '(67)]
    (split-with-interaction q [1 2 3]) => ['() '(1 2 3)]
    (split-with-interaction q [1.2 3.4 5/6]) => ['(1.2 3.4 5/6) '()]

    (split-with-interaction 1.2 [1.2 3.4 5 67 q]) =>  [`(1.2 3.4 5 67 ~q) '()]
    (split-with-interaction 11 [1.2 3.4 5 67 q]) =>  [`(1.2 3.4 5 67) `(~q)]
    (split-with-interaction 11 [q q]) =>  [`() `(~q ~q)]
  ))



; (def p (->Qlosure
;             "«+»"                             ;; token
;             {:num number?, :vec vector?}      ;; wants
;             {:num (fn [item] (->Qlosure
;                                 (str "«" item "+_»")
;                                 {:num number?}
;                                 {:num (partial + item)}))
;              :vec (fn [item] (->Qlosure
;                                 (str "«" item "+_»")
;                                 {:vec vector?}
;                                 {:vec (partial into item)}))}
;                                               ;; transformations
;             ))

; (defn first-use-case
;   "determines the method by which a ReQitem will consume an item it
;   wants, returning the key for the outcome function"
;   [actor target]
;   (let [wants (:wants actor)]
;   (if wants
;     (first (first (filter #((second %) target) wants)))
;     nil
;     )))

; ;;(println (first-use-case p 11))

; (defn transformer-to-be-used
;   [actor item]
;   ((first-use-case actor item) (:transformations actor)))

; ;;(println ((transformer-to-be-used p 11) 11))

; (defn consume
;   [actor item]
;   ((transformer-to-be-used actor item) item))

; ;;(println (consume p 11))

; (println (str "the answer when «+» acts on 11 and then 2 is " (consume (consume p 11) -2/3))) ;; => 13, which is '(consume (consume «+» 11) 2)

; (println (str "the answer when «+» acts on [1 2] and then [3 4] is " (consume (consume p [1 2]) [3 4]))) ;; => [1 2 3 4], which is '(consume (consume «+» 11) 2)
