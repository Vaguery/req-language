(ns req.types-test
  (:use midje.sweet)
  (:use [req.core])
  )

;; basic ReQ types

(fact "the req-type hierarchy works"
  (isa? req :req.core/int :req.core/num) => true
  (isa? req (type 8812) :req.core/int) => true
  (isa? req (type false) :req.core/bool) => true
  (isa? req (type 812N) :req.core/int) => true
  (isa? req (type 812M) :req.core/float) => true
  )

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
  (req-type 88) => :req.core/int
  (req-type false) => :req.core/bool
  (req-type (= 7 7)) => :req.core/bool
  (req-type 8.2) => :req.core/float
  (req-type 882M) => :req.core/float
  (req-type 88N) => :req.core/int
  (req-type 8/9) => :req.core/num
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
  )


;; the req-type of a Qlosure is its return type

;; the req-type of a Nullary is its return type (if any)

;; the req-type of an Immortal is its value

(fact "req-type? returns true or false if the item has the given req-type keyword"
  (req-type? :req.core/int 88) => true
  (req-type? :req.core/bool 88) => false
  (req-type? :req.core/bool false) => true
  (req-type? :req.core/vec [88]) => true
  )

(fact "an Immortal item has the req-type of its :value"
  (req-type (->Immortal 99)) => :req.core/int
  (req-type (->Immortal false)) => :req.core/bool
  (req-type (->Immortal 9/2)) => :req.core/num
  )
