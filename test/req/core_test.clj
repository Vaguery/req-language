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


;; in-place closure design
;; this is a literate design spike, so it's just going to be alternating tests and code here

(defrecord Qlosure [token wants transformations])

(defn wants?
  "determines whether one ReQ item wants another; that is
  whether the first can use the second as an argument to a function"
  [actor target]
  (let [wants (:wants actor)]
  (if wants
    (some? (some #((second %) target) wants))
    false
    )))


(fact "We can tell when any item wants another"
  (wants? 3 4) => false
  (wants? 3 false) => false
  (wants? [1 2] [false :g]) => false
  (wants? (Qlosure. :foo {:arg1 integer?} {:arg1 9}) 4) => true
  (wants? (Qlosure. :foo {:arg1 float? :arg2 float?} {:arg1 9}) 4) => false
  (wants? (Qlosure. :foo {:arg1 some? :arg2 float?} {:arg1 9}) 4) => true
  (wants? (Qlosure. :foo {:arg1 some? :arg2 some?} {:arg1 9}) 4) => true
  (wants? (Qlosure. :foo {:arg1 float? :arg2 float?} {:arg1 9}) 4.2) => true
  (wants? (Qlosure. :foo {:arg1 float? :arg2 float?} {:arg1 9}) 4.2) => true
  )



(defn can-interact?
  "determines whether either of two ReQ items wants the other"
  [a b]
  (or (wants? a b) (wants? b a)))

(fact "We can tell when either of two items wants the other"
  (can-interact? 3 4) => false
  (can-interact? 3 false) => false
  (can-interact? [1 2] [false :g]) => false
  (can-interact? (Qlosure. :foo {:arg1 integer?} {:arg1 8}) 4) => true
  (can-interact? 4 (Qlosure. :foo {:arg1 integer?} {:arg1 8})) => true
  )

(defn all-interactions
  "walks through a collection and returns all things that
  either are wanted by or want the actor"
  [actor items]
    (filter #(can-interact? actor %) items)
    )

(fact "We can tell all the items which an actor wants"
  (let [inter (Qlosure. :foo {:arg1 integer?} {:arg1 8})]
    (all-interactions 3 [1 2 3 4 5 6]) => []
    (all-interactions inter [1 nil -11 3.2 4]) => [1 -11 4]
    (all-interactions (Qlosure. :foo {:arg1 some? :arg2 float?} 8) [1 [nil] -11 [false] 4]) => 
      [1 [nil] -11 [false] 4]
    (all-interactions (Qlosure. :foo {:arg1 float? :arg2 float?} 8) [1 [nil] -11 [false] 4]) => []
    (all-interactions (Qlosure. :foo {:arg1 float? :arg2 #(= 4 %)} 8) [1.2 [nil] -11 [false] 4]) =>
      [1.2 4]
    (all-interactions 3 [1 2 3 4 5 6 inter]) => [inter]
    (all-interactions inter [1.2 nil -11 3.2 4]) => [-11 4]
  ))


;; we want this function because clojure.core/split-with works in a certain way
(defn do-not-interact?
  "determines whether either of two ReQ items wants the other"
  [a b]
  (not (can-interact? a b)))

(defn split-with-interaction
  "splits a collection and returns a vector of two parts:
  the first contains only things that do not interact with the
  actor, the second begins with the first item that does interact
  with the actor; if none of them interact with the actor, the first
  collection will be empty"
  [actor items]
  (split-with (partial do-not-interact? actor) items))


(fact "We get two piles"
  (let [inter (Qlosure. :foo {:arg1 integer?} 8)]
    (split-with-interaction 3 [1 2 3 4 5 6]) => ['(1 2 3 4 5 6) '()]
    (split-with-interaction inter [1.2 3.4 5 67]) => ['(1.2 3.4) '(5 67)]
    (split-with-interaction inter [1.2 3.4 false 67]) => ['(1.2 3.4 false) '(67)]
    (split-with-interaction inter [1 2 3]) => ['() '(1 2 3)]
    (split-with-interaction inter [1.2 3.4 5/6]) => ['(1.2 3.4 5/6) '()]

    (split-with-interaction 1.2 [1.2 3.4 5 67 inter]) =>  [`(1.2 3.4 5 67 ~inter) '()]
    (split-with-interaction 11 [1.2 3.4 5 67 inter]) =>  [`(1.2 3.4 5 67) `(~inter)]
    (split-with-interaction 11 [inter inter]) =>  [`() `(~inter ~inter)]
  ))


;; So let's explore the implications of using a symbol like :+ as a token.
;; It's a req-function, meaning it may have multiple wants: numbers, collections,
;; strings, etc.
;;
;; What's the sequence of transformations like?
;; 
;; :+ wants: Number, Collection, String, Char, &c (an OR-list, in other words)
;; Qlosure(:+, Number) wants: Number->Number, Vector-of-Numbers->V-o-N
;; Qlosure(:+, Collection) wants: Collection->Collection
;; Qlosure(:+, String) wants: String->String
;; Qlosure(:+, Char) wants: Char->String


(defrecord ReQfunction [token wants transformations]
  Object
  (toString [_] 
    (str "«" token "»")))

(def p (->ReQfunction
            "«+»"                ;; token
            {:arg1 number?}   ;; wants
            {:arg1 (fn [item] (->Qlosure
                                (str "«" item "+_»")
                                {:arg1 number?}
                                {:arg1 (partial + item)}))}
                              ;; transformations
            ))

(defn first-use-case
  "determines the method by which a ReQitem will consume an item it
  wants, returning the key for the outcome function"
  [actor target]
  (let [wants (:wants actor)]
  (if wants
    (first (first (filter #((second %) target) wants)))
    nil
    )))

;;(println (first-use-case p 11))

(defn transformer-to-be-used
  [actor item]
  ((first-use-case actor item) (:transformations actor)))

;;(println ((transformer-to-be-used p 11) 11))

(defn consume
  [actor item]
  ((transformer-to-be-used actor item) item))

;;(println (consume p 11))

(println (consume (consume p 11) 2)) ;; => 13, which is ((«+» 11) 2)
