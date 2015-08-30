(ns req.types-test
  (:use midje.sweet
        [req.core :as core]
        [req.types :as types])
  )

;; basic ReQ types


(fact "the ReQ `boolean` function checks _specifically_ for 'true and 'false only"
  (boolean? true) => true  ;; note these aren't the VALUE, just saying if arg is TYPE :bool
  (boolean? 19) => false
  (boolean? []) => false
  (boolean? nil) => false
  (boolean? (= 7 7)) => true)


(fact "the req-type hierarchy works"
  (isa? req :req.types/int :req.types/num) => true
  (isa? req (type 8812) :req.types/int) => true
  (isa? req (type false) :req.types/bool) => true
  (isa? req (type 812N) :req.types/int) => true
  (isa? req (type 812M) :req.types/float) => true)


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
  (req-type 88) => :req.types/int
  (req-type false) => :req.types/bool
  (req-type (= 7 7)) => :req.types/bool
  (req-type 8.2) => :req.types/float
  (req-type 882M) => :req.types/float
  (req-type 88N) => :req.types/int
  (req-type 8/9) => :req.types/num
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
  (type (make-nullary "www" #(constantly "w"))) => req.types.Nullary
  (req-type (make-nullary "www" #(constantly "w"))) => :req.types/nullary)


;; the req-type of an Immortal is its value

(fact "req-type? returns true or false if the item has the given req-type keyword"
  (req-type? :req.types/int 88) => true
  (req-type? :req.types/bool 88) => false
  (req-type? :req.types/bool false) => true
  (req-type? :req.types/vec [88]) => true

  )

(fact "an Immortal item has the req-type of its :value"
  (req-type (->Immortal 99)) => :req.types/int
  (req-type (->Immortal false)) => :req.types/bool
  (req-type (->Immortal 9/2)) => :req.types/num
  )