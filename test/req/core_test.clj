(ns req.core-test
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

(fact "make-qlosure is a convenience function with keywords and defaults"
  (class (make-qlosure "foo")) => req.core.Qlosure
  (:wants (make-qlosure "foo")) => {} ;; not nil
  (:wants (make-qlosure "foo" :wants {:int 88})) => {:int 88}
  (:transitions (make-qlosure "foo" :wants {:int integer?})) => {} ;; not nil
  )

(fact "a Qlosure is created with a token,
    which appears in guillemets when it's printed with (str …)"
  (str (make-qlosure "+")) => "«+»"
  (str (make-qlosure "skeleton")) => "«skeleton»"
  (str (make-qlosure "9+_")) => "«9+_»"
  )


(fact "get-wants produces the :wants component of a Qlosure's :transitions table"
  (get-wants (make-qlosure "+")) => {}
  (get-wants (make-qlosure "+" :wants {:arg1 9})) => {:arg1 9}
  )


(fact "We can tell when (and how) an item `req-wants` another (indicating it can act as an arg)"
  (req-wants 3 4) => false
  (req-wants 3 false) => false
  (req-wants [1 2] [false :g]) => false

  (let [q (make-qlosure :foo :wants {
      :int #(req-type? :int %), 
      :float #(req-type? :float %),
      :vec #(req-type? :vec %)})]
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
  (let [q (make-qlosure
            "foo"
            :wants {:int integer?, :float float?, :vec vector?})]
    (can-interact? q 4) => true
    (can-interact? 4 q) => true
    (can-interact? q q) => false
  ))


(fact "do-not-interact? determines whether either item wants the other"
  (do-not-interact? 3 4) => true
  (do-not-interact? 3 false) => true
  (do-not-interact? [1 2] [false :g]) => true
  (let [q (make-qlosure "s"
            :wants {:int integer?, :float float?, :vec vector?})]
    (do-not-interact? q 4) => false
    (do-not-interact? 4 q) => false
    (do-not-interact? q q) => true
  ))


(fact "all-interacting-items returns everything in a collection that can interact with the (first arg) ReQ item"
  (let [q (make-qlosure :foo
            :wants {:int integer?, :float float?, :vec vector?})]
    (all-interacting-items 3 [1 2 3 4 5 6]) => []
    (all-interacting-items q [1 nil -11 3.2 '(4)]) => [1 -11 3.2]
    (all-interacting-items q []) => []
    (all-interacting-items q [1 1 [1 1] 1]) => [1 1 [1 1] 1]
  ))


(fact "split-with-interaction takes a sequential collection and splits it at the first item that interacts with the actor (arg 1)"
  ;; we will use this to manage ReQ queues
  (fact "if there is no interaction, the first list contains everything"
    (split-with-interaction 3 [1 2 3 4 5 6]) => ['(1 2 3 4 5 6) '()])
    (let [q (make-qlosure :foo
              :wants {:int integer?, :vec vector?})]
      (split-with-interaction q [1.2 3.4 5 67]) => ['(1.2 3.4) '(5 67)]
      (split-with-interaction q [1.2 3.4 false 67]) => ['(1.2 3.4 false) '(67)]
      (split-with-interaction q [1 2 3]) => ['() '(1 2 3)]
      (split-with-interaction q [1.2 3.4 5/6]) => ['(1.2 3.4 5/6) '()]

      (split-with-interaction 1.2 [1.2 3.4 5 67 q]) =>  [`(1.2 3.4 5 67 ~q) '()]
      (split-with-interaction 11 [1.2 3.4 5 67 q]) =>  [`(1.2 3.4 5 67) `(~q)]
      (split-with-interaction 11 [q q]) =>  [`() `(~q ~q)]
    ))


;; Qlosure records

(def p
  "polymorphic 2-ary Qlosure implementing both :num addition and :vec concatenation"
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


(fact "because it has :wants, it can be checked for interactions as intended with req-wants"
  (req-wants p 11) => :num1
  (req-wants p [1 2 3]) => :vec1
  )

(fact "we can use get-transition to... well, you know"
  (get-transition p 11) => (:num1 (:transitions p))
  (get-transition p [11]) => (:vec1 (:transitions p))
  )

(fact "req-consume applies the transformation from a Qlosure to an appropriate item"
  (str (req-consume p 11)) => "«11+⦿»"
  (str (req-consume p [1 2 3])) => "«[1 2 3]+⦿»"
  )

(fact "the result of req-consume can itself be applied to an appropriate item"
  (req-consume (req-consume p 11) 19) => 30
  (req-consume (req-consume p [1 2]) [3]) => [1 2 3]
  )

;; 
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

(fact "a ReQ interpreter with a Qlosure with no targets just keeps it"
  (readable-queue (step (req-with [p false]))) => ["false", "«+»"]
  (readable-queue (step (req-with [p]))) => ["«+»"]
)

(fact "a Qlosure will consume the nearest argument and produce an intermediate"
  (readable-queue (step (req-with [p 1 2 4 8]))) => ["2" "4" "8" "«1+⦿»"]
  (:queue (step (step (req-with [p 1 2 4 8])))) => [4 8 3]
  (:queue (step (step (step (req-with [p 1 2 4 8]))))) => [8 3 4]
  )

(fact "two Qlosure items will consume arguments according to the queue dynamics"
  (let [two-ps (req-with [p 1 p 2 4 8])]
    (str (last (:queue (step two-ps)))) => "«1+⦿»"
    (readable-queue (nth-step two-ps 1)) =>  ["«+»" "2" "4" "8" "«1+⦿»"]
    (readable-queue (nth-step two-ps 2)) =>  ["4" "8" "«1+⦿»" "«2+⦿»"]
    (readable-queue (nth-step two-ps 3)) =>  ["«2+⦿»" "8" "5"]
    (readable-queue (nth-step two-ps 4)) =>  ["5" "10"]
  ))

(fact "Qlosure items will still skip (and requeue) unwanted items as needed"
  (let [two-ps (req-with [p 1 p false 4 8])]
    (readable-queue (nth-step two-ps 1)) =>  ["«+»" "false" "4" "8" "«1+⦿»"]
    (readable-queue (nth-step two-ps 2)) =>  ["8" "«1+⦿»" "false" "«4+⦿»"]
    (readable-queue (nth-step two-ps 3)) =>  ["false" "«4+⦿»" "9"]
    (readable-queue (nth-step two-ps 4)) =>  ["«4+⦿»" "9" "false"]
    (readable-queue (nth-step two-ps 5)) =>  ["false" "13"]
  ))


;; the following is NOT correct for the final ReQ language spec: Qlosures should act as viable arguments for other Qlosures, if compatible
; (fact "stepping through with two Qlosures on the interpreter"
;   (let [two-ps (req-with [p p 1 2 4 8])]
;     (readable-queue (step two-ps)) => ["2" "4" "8" "«+»" "«1+⦿»"]
;     (readable-queue (step (step two-ps))) => ["«1+⦿»" "4" "8" "«2+⦿»"]
;     (readable-queue (step (step (step two-ps)))) => ["8" "«2+⦿»" "5"]
;     (readable-queue (step (step (step (step two-ps))))) => ["5" "10"]
;   ))


;; a simplifying constructor:

;; TODO make this work with req-type?

(fact "testing"
  (req-type? :int 88) => true
  (req-type? :bool false) => true
  )

(defn make-binary-arithmetic-qlosure
  [token operator]
  (make-qlosure
    token
    :wants
      {:num number?}
    :transitions
      {:num 
        (fn [item]
          (make-qlosure
            (str item token "⦿")
            :wants {:num number?}
            :transitions {:num (partial operator item)}))
         }
  ))

(def «-»
  "2-ary Qlosure implementing (Clojure safe) :num subtraction"
  (make-binary-arithmetic-qlosure "-" -')) 

(fact "an arithmetic Qlosure permits quick definition of 2-number ReQ math functions"
  (let [two-ps (req-with [«-» 1 «-» false 4 8])]
    (readable-queue (nth-step two-ps 1)) =>  ["«-»" "false" "4" "8" "«1-⦿»"]
    (readable-queue (nth-step two-ps 2)) =>  ["8" "«1-⦿»" "false" "«4-⦿»"]
    (readable-queue (nth-step two-ps 3)) =>  ["false" "«4-⦿»" "-7"]
    (readable-queue (nth-step two-ps 4)) =>  ["«4-⦿»" "-7" "false"]
    (readable-queue (nth-step two-ps 5)) =>  ["false" "11"]
  ))

(def «*»
  "2-ary Qlosure implementing (Clojure safe) :num multiplication"
  (make-binary-arithmetic-qlosure "*" *')) 

(fact "each _instance_ of a 2-number ReQ math function acquires arguments independently as the interpreter steps forward"
  (let [two-ps (req-with [«-» 1 «*» false 4 8])]
    (readable-queue (nth-step two-ps 1)) =>  ["«*»" "false" "4" "8" "«1-⦿»"]
    (readable-queue (nth-step two-ps 2)) =>  ["8" "«1-⦿»" "false" "«4*⦿»"]
    (readable-queue (nth-step two-ps 3)) =>  ["false" "«4*⦿»" "-7"]
    (readable-queue (nth-step two-ps 4)) =>  ["«4*⦿»" "-7" "false"]
    (readable-queue (nth-step two-ps 5)) =>  ["false" "-28"]
  ))


;; the `boolean?` helper

(fact "the ReQ `boolean` function checks _specifically_ for 'true and 'false only"
  (boolean? true) => true  ;; note these aren't the VALUE, just saying if arg is TYPE :bool
  (boolean? 19) => false
  (boolean? []) => false
  (boolean? nil) => false
  (boolean? (= 7 7)) => true
  )

;; using the make-binary-logical-qlosure helper to create the boolean equivalent of arithmetic

;; TODO make this work with req-type?

(defn make-binary-logical-qlosure
  [token operator]
  (make-qlosure
    token   
    :wants 
      {:bool boolean?}
    :transitions
      {:bool 
        (fn [item]
          (make-qlosure
              (str item token "⦿")
              :wants {:bool boolean?}
              :transitions {:bool (partial operator item)}))
         }
  ))

(def «∧»
  "2-ary Qlosure implementing :bool AND"
  (make-binary-logical-qlosure "∧" ∧))

(def «∨»
  "2-ary Qlosure implementing :bool OR"
  (make-binary-logical-qlosure "∨" ∨)) 

(fact "these «∧» and «∨» Qlosures implement binary boolean AND and OR respectively"
  (let [two-ps (req-with [«∧» true «∨» true false «∨» true false true])]
    (readable-queue (nth-step two-ps 1)) => 
      ["«∨»" "true" "false" "«∨»" "true" "false" "true" "«true∧⦿»"]
    (readable-queue (nth-step two-ps 2)) => 
      ["false" "«∨»" "true" "false" "true" "«true∧⦿»" "«true∨⦿»"]
    (readable-queue (nth-step two-ps 3)) => 
      ["true" "false" "true" "«true∧⦿»" "«true∨⦿»" "«false∨⦿»"]
    (readable-queue (nth-step two-ps 4)) => 
      ["«true∨⦿»" "«false∨⦿»" "false" "true" "true"]
    (readable-queue (nth-step two-ps 5)) => 
      ["true" "true" "«false∨⦿»" "true"]
  ))


;; using the arithmetic and boolean examples, the core `make-binary-one-type-qlosure`
;; function will build any Qlosure with same-typed arguments quickly


(def «∧»
  "2-ary Qlosure implementing :bool AND"
  (make-binary-one-type-qlosure "∧" :bool ∧)) 
(def «∨»
  "2-ary Qlosure implementing :bool OR"
  (make-binary-one-type-qlosure "∨" :bool ∨)) 


(fact "these new definitions still implement boolean AND and OR, respectively"
  (let [two-ps (req-with [«∧» true «∨» true false «∨» true false true])]
    (readable-queue (nth-step two-ps 1)) => 
      ["«∨»" "true" "false" "«∨»" "true" "false" "true" "«true∧⦿»"]
    (readable-queue (nth-step two-ps 2)) => 
      ["false" "«∨»" "true" "false" "true" "«true∧⦿»" "«true∨⦿»"]
    (readable-queue (nth-step two-ps 3)) => 
      ["true" "false" "true" "«true∧⦿»" "«true∨⦿»" "«false∨⦿»"]
    (readable-queue (nth-step two-ps 4)) => 
      ["«true∨⦿»" "«false∨⦿»" "false" "true" "true"]
    (readable-queue (nth-step two-ps 5)) => 
      ["true" "true" "«false∨⦿»" "true"]
  ))


(def «*»
  "binary Qlosure implementing (Clojure safe) multiplication"
  (make-binary-one-type-qlosure "*" :num *'))

(def «-»
  "binary Qlosure implementing (Clojure safe) subtraction"
  (make-binary-one-type-qlosure "-" :num -')) 


(fact "the `make-binary-one-type-qlosure` function works for arithmetic too"
  (let [two-ps (req-with [«-» 1 «*» false 4 8])]
    (readable-queue (nth-step two-ps 1)) => ["«*»" "false" "4" "8" "«1-⦿»"]
    (readable-queue (nth-step two-ps 2)) => ["8" "«1-⦿»" "false" "«4*⦿»"]
    (readable-queue (nth-step two-ps 3)) => ["false" "«4*⦿»" "-7"]
    (readable-queue (nth-step two-ps 4)) => ["«4*⦿»" "-7" "false"]
    (readable-queue (nth-step two-ps 5)) => ["false" "-28"]
  ))


(def «≤»
  "binary Qlosure returns :bool based on whether its 2 :num arguments satisfy (arg1 ≤ arg2)"
  (make-binary-one-type-qlosure "≤" :num <=)) 


(fact "`make-binary-one-type-qlosure` works for ad hoc definitions, too"
  (let [two-ps (req-with [«≤» 1 «∨» false 4 8])]
    (readable-queue (nth-step two-ps 1)) =>  ["«∨»" "false" "4" "8" "«1≤⦿»"]
    (readable-queue (nth-step two-ps 2)) =>  ["4" "8" "«1≤⦿»" "«false∨⦿»"]
    (readable-queue (nth-step two-ps 3)) =>  ["«false∨⦿»" "8" "true"]
    (readable-queue (nth-step two-ps 4)) =>  ["8" "true"]
  ))

;; more Qlosure-making helper functions: `make-unary-qlosure`

;; TODO fix these to use (req-type?)

(def «neg»
  "defines a Qlosure that negates a single :num argument"
  (make-unary-qlosure "neg" :num -)) 

(fact "a unary Qlosure can be built wit `make-unary-qlosure"
  (let [negger (req-with [«neg» 1 «neg» false 4 8])]
    (readable-queue (nth-step negger 1)) =>  ["«neg»" "false" "4" "8" "-1"]
    (readable-queue (nth-step negger 2)) =>  ["8" "-1" "false" "-4"]
  ))


(def «neg-even?»
  "creates a Qlosure that answers `true` if its :int argument is negative AND even"
  (make-unary-qlosure "neg-even?" :int
  (partial #(and (neg? %) (even? %)))))


(fact "unique ad hoc unary qlosures can also be made and used"
  (let [negger (req-with [«neg-even?» -1 «neg-even?» «neg-even?» false -4 8])]
    (readable-queue (nth-step negger 1)) =>  
      ["«neg-even?»" "«neg-even?»" "false" "-4" "8" "false"]
    (readable-queue (nth-step negger 2)) =>  
      ["8" "false" "«neg-even?»" "false" "true"]
    (readable-queue (nth-step negger 3)) =>  
      ["false" "true" "false" "false"]
  ))

;; multiple result values

(def «3x»
  "a 1-ary Qlosure which produces three copies of its :any argument"
  (make-unary-qlosure "3x" :any
  (partial #(list % % %))))


(fact "multiple return values (returned as a Clojure list) are pushed onto the queue"
  (let [tripler (req-with [«3x» -1 «3x» «3x» false -4 8])]
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
      ["-1" "-1" "-1" "-1" "-1" "-1" "-1" "-1" "-1" "false" "false" "false" "false" "false" "false" "false" "false" "false" "-4" "-4" "-4" "-4" "-4" "-4" "-4" "-4" "-4" "8" "8" "8" "8" "8" "8" "8" "8" "8" "«3x»" "«3x»" "«3x»"]
  ))


(def «3xVec»
  "1-ary Qlosure which produces a Clojure vector containing 3x copies of its :any arg"
  (make-unary-qlosure "3xVec" :any
  (partial #(list (vector % % %)))))


(fact "for a collection result to be handled correctly by the ReQ interpreter,
  the :transition function should wrap it in a list"
  (let [tripler (req-with [«3xVec» -1  false «3xVec» -4 «3xVec» 8])]
    (readable-queue (step tripler)) =>
      ["false" "«3xVec»" "-4" "«3xVec»" "8" "[-1 -1 -1]"]
    (readable-queue (nth-step tripler 2)) =>  
      ["-4" "«3xVec»" "8" "[-1 -1 -1]" "[false false false]"]
    (readable-queue (nth-step tripler 3)) =>  
      ["8" "[-1 -1 -1]" "[false false false]" "[-4 -4 -4]"]
  ))


;; Nullary items

(fact "a Nullary has a token it shows when printed, like a Qlosure"
  (str (make-nullary "beep" nil)) => "«beep»"
  )


(def silly
  "a Nullary which produces the number 29 when executed"
  (make-nullary "29" (constantly 29))) ;; definitely NOT an exciting example!


(fact "a Nullary produces its result (which is pushed) when it is in the hot seat"
  (readable-queue (req-with [silly false])) => ["«29»" "false"]
  (readable-queue (step (req-with [silly false]))) => ["false" "29"]
  )

;; Nullary "seeds"

(def eights
  "a Nullary which produces itself and a number 8 every time it's executed"
  (make-seed "8s" 8))


(fact "a Nullary produces its result (which is pushed) when it is in the hot seat"
  (let [eighty (req-with [eights false])]
    (readable-queue eighty) => ["«8s»" "false"]
    (readable-queue (step eighty)) => ["false" "«8s»" "8"]
    (readable-queue (nth-step eighty 2)) => ["«8s»" "8" "false"]
    (readable-queue (nth-step eighty 3)) => ["8" "false" "«8s»" "8"]
  ))

;; Nullary "timers"

(def c
  "a simple timer that counts from 2 to 4, then disappears and becomes 4"
  (make-timer 2 4))

(fact "a Nullary timer advances at every execution, and eventually becomes its end value"
  (readable-queue (req-with [c false])) => ["«timer:2-4»" "false"]
  (readable-queue (step (req-with [c false]))) => ["false" "«timer:3-4»"]
  (readable-queue (step (step (req-with [c false])))) => ["«timer:3-4»" "false"]
  (readable-queue (step (step (step (req-with [c false]))))) => ["false" "4"]
)


(def c
  "a Nullary timer with a payload: it emits :foo on every execution"
  (make-timer 2 11 :foo))


(fact "a Nullary timer with a payload emits its payload every cycle, then dissolves"
  (let [timey (req-with [c false])]
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
      [":foo" ":foo" ":foo" "false" "11" ":foo" ":foo" ":foo" ":foo" ":foo"]
))

;; Nullary loopers

(def jenny
  "a Nullary looper that eternally cycles through its vector contents, emitting the top item each step"
  (make-looper [9 0 3 5 7 6 8]))


(fact "a Nullary looper cycles its (sequence) payload, emitting the top item each step"
  (let [her-number (req-with [jenny false])]
    (readable-queue (nth-step her-number 0 )) => ["«[9 0 3 5 7 6 8]»" "false"]
    (readable-queue (nth-step her-number 1 )) => ["false" "«[0 3 5 7 6 8 9]»" "9"]
    ;; ... much later ...
    (readable-queue (nth-step her-number 1202 )) =>
      ["8" "6" "7" "5" "3" "0" "9" "8" "6" "7" "5" "3" "0" "9" "8" "6" "7" "5" "3" "0" "9" "false" "«[8 9 0 3 5 7 6]»" "6" "7" "5" "3" "0" "9" "8" "6" "7" "5" "3" "0" "9" "8" "6" "7" "5" "3" "0" "9" "8" "6" "7" "5" "3" "0" "9"]
))

;; EXPLORATORY

;; Immortal items

(fact "req-type? returns true or false if the item has the given req-type keyword"
  (req-type? :int 88) => true
  (req-type? :bool 88) => false
  (req-type? :bool false) => true
  (req-type? :vec [88]) => true
  )

(fact "an Immortal item has the req-type of its :value"
  (req-type (->Immortal 99)) => :int
  (req-type (->Immortal false)) => :bool
  (req-type (->Immortal 9/2)) => :num
  )

(fact "an Immortal item prints with the ⥀ character appended"
  (str (->Immortal 99)) => "99⥀"
  (str (->Immortal false)) => "false⥀"
  (str (->Immortal [1 [2]])) => "[1 [2]]⥀"
  )

(def «stringer»
  "a 1-ary Qlosure which wants an :int and applies `#(str %)`"
  (make-unary-qlosure "stringer" :int (partial #(str %))))


; (fact "a Qlosure that wants a req-type also will want an Immortal of that req-type"
;   (let [stringy (req-with [«stringer» false (->Immortal 88)])]
;   (readable-queue (nth-step stringy 0)) => ["«stringer»" "false" "88⥀"]
;   (readable-queue (nth-step stringy 1)) => ["false" "\"88\"" "88⥀"]
;   ))