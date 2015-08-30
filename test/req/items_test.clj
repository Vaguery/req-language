(ns req.items-test
  (:use midje.sweet
        [req.core]
        [req.items])
  )

;; basic ReQ types


(fact "the ReQ `boolean` function checks _specifically_ for 'true and 'false only"
  (boolean? true) => true  ;; note these aren't the VALUE, just saying if arg is TYPE :bool
  (boolean? 19) => false
  (boolean? []) => false
  (boolean? nil) => false
  (boolean? (= 7 7)) => true)


(fact "the req-type hierarchy works"
  (isa? req :req.items/int :req.items/num) => true
  (isa? req (type 8812) :req.items/int) => true
  (isa? req (type false) :req.items/bool) => true
  (isa? req (type 812N) :req.items/int) => true
  (isa? req (type 812M) :req.items/float) => true)


;; individual literal items

(fact "req-type checking works for individual things"
  (req-int? 88) => true
  (req-num? 88) => true
  (req-bool? false) => true
  (req-bool? (= 7 7)) => true
  (req-float? 8.2) => true
  (req-float? 882M) => true
  (req-float? 882) => false
  (req-num? 88N) => true
  (req-int? 88N) => true
  (req-int? 8/9) => false
  (req-num? 8/9) => true
  )

(fact "req-type? returns the type as such"
  (req-type 88) => :req.items/int
  (req-type false) => :req.items/bool
  (req-type (= 7 7)) => :req.items/bool
  (req-type 8.2) => :req.items/float
  (req-type 882M) => :req.items/float
  (req-type 88N) => :req.items/int
  (req-type 8/9) => :req.items/num
  )

;; the req-type of collections

(fact "req-vec? can also check the subtype of the contents given an optional arg"
  (req-vec? [1 2 3]) => true
  (req-vec? req-int? [1 2 3]) => true
  (req-vec? req-bool? [1 2 3]) => false
  (req-vec? req-bool? [false true false]) => true
  (req-vec? req-num? [1 2.3 4/5]) => true
  (req-vec? req-num? []) => true
  (req-vec? req-bool? []) => true
  (req-vec? req-int? [8 [9 10]]) => false
  )

;; req-tree?
;; req-list?
;; req-set?
;; req-queue?
;; req-interpreter?

;; the req-type of a Qlosure is its return type

;; the req-type of a Nullary is ::nullary

(fact "the `nullary?` helper detects Nullary objects"
  (nullary? (make-nullary "www" #(constantly "w"))) => true
  (nullary? 88) => false)


(fact "Nullary ReQ items have req-type ::nullary"
  (type (make-nullary "www" #(constantly "w"))) => req.items.Nullary
  (req-type (make-nullary "www" #(constantly "w"))) => :req.items/nullary)


;; the req-type of an Immortal is its value

(fact "req-type? returns true or false if the item has the given req-type keyword"
  (req-type? :req.items/int 88) => true
  (req-type? :req.items/bool 88) => false
  (req-type? :req.items/bool false) => true
  (req-type? :req.items/vec [88]) => true

  )

(fact "an Immortal item has the req-type of its :value"
  (req-type (->Immortal 99)) => :req.items/int
  (req-type (->Immortal false)) => :req.items/bool
  (req-type (->Immortal 9/2)) => :req.items/num
  )

;; Qlosures are the core of ReQ's approach to partial application of functions.
;; Every instruction, function or partially-applied "closure" is a ReQ item
;; on the queue. In addition to a "token" which represents it in print, each Qlosure
;; has a :wants map, which names the several arguments it can accept and
;; assigns a "checker" for each of those arguments which is used to determine whether
;; some other ReQ item can act as an argument. Qlosures also have a :transitions
;; map, which uses the same keys as :wants, and indicates what should be "made"
;; if the Qlosure item "finds" a specified argument. In the case of a unary function,
;; this :transition simply indicates the result of applying a function to the argument;
;; in the case of a binary or more complex function, the transition may specify one or
;; more Qlosures which are created in turn.
;;
;; Functions defined as Qlosure items are expected to be polymorphic. For example, a
;; "+" function may accent a ::num or a ::vec argument, and the resulting Qlosure this
;; creates will differ in each case. The ::num version will want another number to add
;; to the first; the ::vec version will look for another vector to concatenate.
;;
;; By design, ReQ Qlosures are where the "user modeling" happens. Against a backdrop of
;; core functionality one expects from any algorithmic system (arithmetic, logic, string-
;; handling, collections), the user can define new domain-specific types, and more
;; importantly a suite of _functions_ which connect these new types to the existing core
;; through new Qlosure definitions.


(fact "a Qlosure is created with a token, which appears in guillemets when it's printed with (str …)"
  (str (make-qlosure "+")) => "«+»"
  (str (make-qlosure "skeleton")) => "«skeleton»"
  (str (make-qlosure "9+_")) => "«9+_»")


(fact "make-qlosure is a convenience function that makes a Qlosure with the specified token"
  (class (make-qlosure "foo")) => req.items.Qlosure
  (str (make-qlosure "foo")) => "«foo»")


(fact "make-qlosure takes an explicit :wants keyword to set that value"
  (:wants (make-qlosure "foo")) => {} ;; not nil!
  (:wants (make-qlosure "foo" :wants {:arg1 88})) => {:arg1 88})


(fact "make-qlosure takes an explicit :transitions keyword to set that value"
  (:transitions (make-qlosure "foo")) => {} ;; not nil
  (:transitions (make-qlosure "foo" :transitions {:a :b})) => {:a :b})


(fact "make-qlosure takes an explicit :type keyword to set that value"
  (:type (make-qlosure "foo")) => :req.items/thing ;; not ::qlosure!
  (:type (make-qlosure "foo" :type :req.items/int)) => :req.items/int)


;; how ReQ items interact with :wants


(fact "literals don't really have 'wants'"
  (req-wants 3 4) => false
  (req-wants 3 false) => false
  (req-wants [1 2] [false :g]) => false)


(fact "get-wants produces the :wants component of a Qlosure's :transitions table"
  (get-wants (make-qlosure "+")) => {}
  (get-wants (make-qlosure "+" :wants {:arg1 9})) => {:arg1 9})


(fact "Qlosure items with wants use those to check potential arguments"
  (let [q (make-qlosure
          :foo
          :wants {
            :int req-int?, 
            :float req-float?,
            :vec req-vec?})]
    (req-wants q 4) => truthy       ;; :int
    (req-wants q false) => falsey   ;; false
    (req-wants q [1 2 3]) => truthy ;; :vec
    (req-wants q "nope") => falsey  ;; false
    (req-wants q 4.3) => truthy))


(fact "the value returned by req-wants is actually the key of the triggered :want item"
  (let [q (make-qlosure
          :foo
          :wants {
            :int req-int?, 
            :float req-float?,
            :vec req-vec?})]
    (req-wants q 4) => :int
    (req-wants q false) => falsey   ;; false
    (req-wants q [1 2 3]) => :vec
    (req-wants q "nope") => falsey  ;; false
    (req-wants q 4.3) => :float))


(fact "`req-wants` returns the _first_ key in an item's :wants which matches the item"
  (let [q (make-qlosure
          :foo
          :wants {
            :int req-int?, 
            :num req-num?})]
    (req-wants q 4) => :int
    (req-wants q 4.3) => :num))


(fact "can-interact? determines whether _either_ ReQ item wants the other"
  (can-interact? 3 4) => false
  (can-interact? 3 false) => false
  (can-interact? [1 2] [false :g]) => false
  (let [q (make-qlosure
            "foo"
            :wants {:int #(isa? req (class %) :req.items/int), :float float?, :vec vector?})]
    (can-interact? q 4) => true
    (can-interact? 4 q) => true
    (can-interact? q q) => false))


(fact "do-not-interact? determines whether either item wants the other"
  (do-not-interact? 3 4) => true
  (do-not-interact? 3 false) => true
  (do-not-interact? [1 2] [false :g]) => true
  (let [q (make-qlosure "s"
            :wants {:int integer?, :float float?, :vec vector?})]
    (do-not-interact? q 4) => false
    (do-not-interact? 4 q) => false
    (do-not-interact? q q) => true))


(fact "all-interacting-items returns everything in a collection that can interact with the (first arg) ReQ item"
  (let [q (make-qlosure :foo
            :wants {:int integer?, :float float?, :vec vector?})]
    (all-interacting-items 3 [1 2 3 4 5 6]) => []
    (all-interacting-items q [1 nil -11 3.2 '(4)]) => [1 -11 3.2]
    (all-interacting-items q []) => []
    (all-interacting-items q [1 1 [1 1] 1]) => [1 1 [1 1] 1]))


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
      (split-with-interaction 11 [q q]) =>  [`() `(~q ~q)]))


;; Qlosure as such


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


(fact "we can determine whether a ReQ item is a qlosure using `qlosure?`"
  (qlosure? p) => true
  (qlosure? 88) => false)


(fact "we can use get-transition to... well, you know"
  (get-transition p 11) => (:num1 (:transitions p))
  (get-transition p [11]) => (:vec1 (:transitions p)))


(fact "we can use req-type to get the type of the Qlosure (like any other ReQ item)"
  (req-type p) => :req.items/thing
  (req-type (make-qlosure "x" :type (req-type 9.2))) => :req.items/float)


(fact "req-consume applies the transformation from a Qlosure to an appropriate item"
  (str (req-consume p 11)) => "«11+⦿»"
  (str (req-consume p [1 2 3])) => "«[1 2 3]+⦿»")


(fact "the result of req-consume can itself be applied to an appropriate item"
  (req-consume (req-consume p 11) 19) => 30
  (req-consume (req-consume p [1 2]) [3]) => [1 2 3])


(fact "applying req-consume to an unwanted item produces a list containing the args"
  (req-consume p false) => (just [p false]))




;; Immortal items

(fact "an Immortal item prints with the ⥀ character appended"
  (str (->Immortal 99)) => "99⥀"
  (str (->Immortal false)) => "false⥀"
  (str (->Immortal [1 [2]])) => "[1 [2]]⥀"
  )

(def «stringer»
  "a 1-ary Qlosure which wants an :req.items/int and applies `#(str %)`"
  (make-unary-qlosure "stringer" :req.items/int (partial #(str %))))


; (fact "a Qlosure that wants a req-type also will want an Immortal of that req-type"
;   (let [stringy (req-with [«stringer» false (->Immortal 88)])]
;   (readable-queue (nth-step stringy 0)) => ["«stringer»" "false" "88⥀"]
;   (readable-queue (nth-step stringy 1)) => ["false" "\"88\"" "88⥀"]
;   ))