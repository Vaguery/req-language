(ns req.items-test
  (:use midje.sweet
        [req.core]
        [req.interpreter]
        [req.items]
        [req.instructions.bool]))


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
  (req-num? 8/9) => true)


(fact "req-type? returns the type as such"
  (req-type 88) => :req.items/int
  (req-type false) => :req.items/bool
  (req-type (= 7 7)) => :req.items/bool
  (req-type 8.2) => :req.items/float
  (req-type 882M) => :req.items/float
  (req-type 88N) => :req.items/int
  (req-type 8/9) => :req.items/num)


;; the req-type of collections

(fact "req-vec? can also check the subtype of the contents given an optional arg"
  (req-vec? [1 2 3]) => true
  (req-vec? req-int? [1 2 3]) => true
  (req-vec? req-bool? [1 2 3]) => false
  (req-vec? req-bool? [false true false]) => true
  (req-vec? req-num? [1 2.3 4/5]) => true
  (req-vec? req-num? []) => true
  (req-vec? req-bool? []) => true
  (req-vec? req-int? [8 [9 10]]) => false)


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


;; Qlosures


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


(def «-»
  "2-ary Qlosure implementing (Clojure safe) :num subtraction"
  (make-binary-one-type-qlosure "-" :req.items/num -')) 


(fact "an arithmetic Qlosure permits quick definition of 2-number ReQ math functions"
  (let [two-ps (req-with [«-» 1 «-» false 4 8])]
    (readable-queue (nth-step two-ps 1)) =>  ["«-»" "false" "4" "8" "«1-⦿»"]
    (readable-queue (nth-step two-ps 2)) =>  ["8" "«1-⦿»" "false" "«4-⦿»"]
    (readable-queue (nth-step two-ps 3)) =>  ["false" "«4-⦿»" "-7"]
    (readable-queue (nth-step two-ps 4)) =>  ["«4-⦿»" "-7" "false"]
    (readable-queue (nth-step two-ps 5)) =>  ["false" "11"]))


(def «*»
  "2-ary Qlosure implementing (Clojure safe) :num multiplication"
  (make-binary-one-type-qlosure  "*" :req.items/num *')) 


(fact "each _instance_ of a 2-number ReQ math function acquires arguments independently as the interpreter steps forward"
  (let [two-ps (req-with [«-» 1 «*» false 4 8])]
    (readable-queue (nth-step two-ps 1)) =>  ["«*»" "false" "4" "8" "«1-⦿»"]
    (readable-queue (nth-step two-ps 2)) =>  ["8" "«1-⦿»" "false" "«4*⦿»"]
    (readable-queue (nth-step two-ps 3)) =>  ["false" "«4*⦿»" "-7"]
    (readable-queue (nth-step two-ps 4)) =>  ["«4*⦿»" "-7" "false"]
    (readable-queue (nth-step two-ps 5)) =>  ["false" "-28"]))


(def «∧»
  "2-ary Qlosure implementing :bool AND"
  (make-binary-one-type-qlosure "∧" :req.items/bool ∧))


(def «∨»
  "2-ary Qlosure implementing :bool OR"
  (make-binary-one-type-qlosure "∨" :req.items/bool ∨)) 


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
      ["true" "true" "«false∨⦿»" "true"]))


(fact "the `make-binary-one-type-qlosure` function works for arithmetic too"
  (let [two-ps (req-with [«-» 1 «*» false 4 8])]
    (readable-queue (nth-step two-ps 1)) => ["«*»" "false" "4" "8" "«1-⦿»"]
    (readable-queue (nth-step two-ps 2)) => ["8" "«1-⦿»" "false" "«4*⦿»"]
    (readable-queue (nth-step two-ps 3)) => ["false" "«4*⦿»" "-7"]
    (readable-queue (nth-step two-ps 4)) => ["«4*⦿»" "-7" "false"]
    (readable-queue (nth-step two-ps 5)) => ["false" "-28"]))


(def «≤»
  "binary Qlosure returns :bool based on whether its 2 :num arguments satisfy (arg1 ≤ arg2)"
  (make-binary-one-type-qlosure "≤" :req.items/num <=)) 


(fact "`make-binary-one-type-qlosure` works for ad hoc definitions, too"
  (let [two-ps (req-with [«≤» 1 «∨» false 4 8])]
    (readable-queue (nth-step two-ps 1)) =>  ["«∨»" "false" "4" "8" "«1≤⦿»"]
    (readable-queue (nth-step two-ps 2)) =>  ["4" "8" "«1≤⦿»" "«false∨⦿»"]
    (readable-queue (nth-step two-ps 3)) =>  ["«false∨⦿»" "8" "true"]
    (readable-queue (nth-step two-ps 4)) =>  ["8" "true"]))


;; more Qlosure-making helper functions: `make-unary-qlosure`


(def «neg»
  "defines a Qlosure that negates a single :req.items/num argument"
  (make-unary-qlosure "neg" :req.items/num -))


(fact "a unary Qlosure can be built wit `make-unary-qlosure"
  (let [negger (req-with [«neg» 1 «neg» false 4 8])]
    (readable-queue (nth-step negger 1)) =>  ["«neg»" "false" "4" "8" "-1"]
    (readable-queue (nth-step negger 2)) =>  ["8" "-1" "false" "-4"]))


;; showing off how to make an ad hoc Qlosure item with an unusual function


(def «neg-even?»
  "creates a Qlosure that answers `true` if its :int argument is negative AND even"
  (make-unary-qlosure "neg-even?" :req.items/int
  (partial #(and (neg? %) (even? %)))))


(fact "unique ad hoc unary qlosures can also be made and used"
  (let [negger (req-with [«neg-even?» -1 «neg-even?» «neg-even?» false -4 8])]
    (readable-queue (nth-step negger 1)) =>  
      ["«neg-even?»" "«neg-even?»" "false" "-4" "8" "false"]
    (readable-queue (nth-step negger 2)) =>  
      ["8" "false" "«neg-even?»" "false" "true"]
    (readable-queue (nth-step negger 3)) =>  
      ["false" "true" "false" "false"]))


;; Immortal items


(fact "the `immortalize` function returns an Immortal with a given value"
  (type (immortalize 99)) => req.items.Immortal)


(fact "Immortality can't be 'stacked'"
  (str (immortalize (immortalize 123.456))) => "123.456⥀") ;; not "123.456⥀⥀"


(fact "req-type? returns true or false if the item has the given req-type keyword"
  (req-type? :req.items/int 88) => true
  (req-type? :req.items/bool 88) => false
  (req-type? :req.items/bool false) => true
  (req-type? :req.items/vec [88]) => true)


(fact "an Immortal item has the req-type of its :value"
  (req-type (immortalize 99)) => :req.items/int
  (req-type (immortalize false)) => :req.items/bool
  (req-type (immortalize 9/2)) => :req.items/num)


(fact "an Immortal item prints with the ⥀ character appended"
  (str (immortalize 99)) => "99⥀"
  (str (immortalize false)) => "false⥀"
  (str (immortalize [1 [2]])) => "[1 [2]]⥀")


(def «doubler»
  "a 1-ary Qlosure which wants an :req.items/int and applies `#(str % %)`"
  (make-unary-qlosure
    "doubler"
    :req.items/int
    (partial #(str % %))))


(fact "the req-type of an Immortal item is its value's req-type"
  (req-type (immortalize 99)) => (req-type 99)
  (req-type (immortalize false)) => (req-type false)
  (req-type (immortalize :a)) => (req-type :a))


(fact "a Qlosure that wants a req-type also will want an Immortal of that req-type"
  (let [big-old-123 (immortalize 123)]
  (type big-old-123) => req.items.Immortal
  (req-type big-old-123) => :req.items/int
  (req-wants «doubler» big-old-123) => :req.items/int
  ))


(fact "a Qlosure that is Immortal still has the same wants as its value"
  (let [always-subtract (immortalize «-»)]
    (req-wants always-subtract 29) => :req.items/num))


(fact "an Immortal item is not consumed by a Qlosure that uses it as an argument"
  (let [big-old-123 (immortalize 123)
        stringy (req-with [«doubler» false «-» big-old-123])]
  (readable-queue (nth-step stringy 0)) => ["«doubler»" "false" "«-»" "123⥀"]
  (readable-queue (nth-step stringy 1)) => ["false" "«-»" "123123" "123⥀"]
  (readable-queue (nth-step stringy 2)) => ["«-»" "123123" "123⥀" "false"]
  (readable-queue (nth-step stringy 3)) => ["false" "123123" "«123-⦿»" "123⥀"]
  (readable-queue (nth-step stringy 4)) => ["123123" "«123-⦿»" "123⥀" "false"]
  (readable-queue (nth-step stringy 5)) => ["«123-⦿»" "123⥀" "false" "123123"]
  (readable-queue (nth-step stringy 6)) => ["false" "123123" "0" "123⥀"]))


(fact "an Immortal item is not consumed when it consumes another item either"
  (let [stringy (req-with [(immortalize «doubler») 100 123 456])]
  (readable-queue (nth-step stringy 0)) => ["«doubler»⥀" "100" "123" "456"]
  (readable-queue (nth-step stringy 1)) => ["123" "456" "100100" "«doubler»⥀"]
  (readable-queue (nth-step stringy 2)) => ["456" "100100" "123123" "«doubler»⥀"]
  (readable-queue (nth-step stringy 3)) => ["100100" "123123" "456456" "«doubler»⥀"]
  (readable-queue (nth-step stringy 4)) => ["123123" "456456" "«doubler»⥀" "100100"]))


(fact "an Immortal item is not consumed when it consumes another Immortal item either"
  (let [big-old-123 (immortalize 123)
        stringy (req-with [(immortalize «doubler») 100 big-old-123 456])]
  (readable-queue (nth-step stringy 0)) => ["«doubler»⥀" "100" "123⥀" "456"]
  (readable-queue (nth-step stringy 1)) => ["123⥀" "456" "100100" "«doubler»⥀"]
  (readable-queue (nth-step stringy 2)) => ["456" "100100" "123123" "«doubler»⥀" "123⥀"]
  (readable-queue (nth-step stringy 3)) => ["123⥀" "100100" "123123" "456456" "«doubler»⥀"]
  (readable-queue (nth-step stringy 4)) =>
    ["100100" "123123" "456456" "123123" "«doubler»⥀" "123⥀"]))


;; Channels


(fact "a Channel has an id and a value, which is shown as '?' if unset"
  (str (->Channel "foo" nil)) => "⬍foo|?⬍"
  (str (->Channel "foo" 77)) => "⬍foo|77⬍"
  )


(fact "`channel?` returns true for Channel items"
  (channel? (->Channel "x" :foo))=> true
  (channel? (->Immortal :foo))=> false
  )


(fact "`silent-channel?` returns true if the argument is an empty channel Channel"
  (silent-channel? (->Channel "x" nil))=> true
  (silent-channel? (->Channel "x" 8182))=> false
  (silent-channel? 8182)=> false)


(fact "`active-channel?` returns true if the argument is an empty channel Channel"
  (active-channel? (->Channel "x" nil))=> false
  (active-channel? (->Channel "x" 8182))=> true
  (active-channel? 8182)=> false)


(fact "the req-type of a Channel is the req-type of its contents"
  (req-type (->Channel "x" 88)) => :req.items/int
  )


(fact "the req-type of an empty Channel is ::channel"
  (req-type (->Channel "x" nil)) => :req.items/channel
  )


(fact "the req-type of a Channel is the req-type of its current contents (not its specifier)"
  (req-type (->Channel "x" 88)) => :req.items/int
  )


(fact "a Channel still has the same wants as its value"
  (let [channel-x (->Channel "x" «-»)]
    (req-wants channel-x 29) => :req.items/num))


(fact "an empty Channel has no wants"
  (let [channel-x (->Channel "x" nil)]
    (req-wants channel-x 29) => false))


(fact "a Channel item is not consumed by a Qlosure that uses its value as an argument"
    (let [channel-x (->Channel "x" 123)
          stringy (req-with [«doubler» false «-» channel-x])]
    (readable-queue (nth-step stringy 0)) => ["«doubler»" "false" "«-»" "⬍x|123⬍"]
    (readable-queue (nth-step stringy 1)) => ["false" "«-»" "123123" "⬍x|123⬍"]
    (readable-queue (nth-step stringy 2)) => ["«-»" "123123" "⬍x|123⬍" "false"]
    (readable-queue (nth-step stringy 3)) => ["false" "123123" "«123-⦿»" "⬍x|123⬍"]
    (readable-queue (nth-step stringy 4)) => ["123123" "«123-⦿»" "⬍x|123⬍" "false"]
    (readable-queue (nth-step stringy 5)) => ["«123-⦿»" "⬍x|123⬍" "false" "123123"]
    (readable-queue (nth-step stringy 6)) => ["false" "123123" "0" "⬍x|123⬍"]
    ))



(fact "a Channel item is not consumed when it consumes another item either"
  (let [stringy (req-with [(->Channel "x" «doubler») 100 123 456])]
  (readable-queue (nth-step stringy 0)) => ["⬍x|«doubler»⬍" "100" "123" "456"]
  ;; ...
  (readable-queue (nth-step stringy 4)) => ["123123" "456456" "⬍x|«doubler»⬍" "100100"]))


(fact "a Channel item is not consumed when it consumes another Channel item either"
  (let [big-old-123 (->Channel "x" 123)
        stringy (req-with [(->Channel "y" «doubler») 100 big-old-123 456])]
  (readable-queue (nth-step stringy 0)) => ["⬍y|«doubler»⬍" "100" "⬍x|123⬍" "456"]
  (readable-queue (nth-step stringy 22)) =>["100100" "123123" "456456" "123123"
                                            "123123" "123123" "123123" "⬍y|«doubler»⬍"
                                            "⬍x|123⬍"]))
