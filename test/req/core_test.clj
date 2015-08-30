(ns req.core-test
  (:use midje.sweet
        [req.interpreter]
        [req.core]
        [req.items]
        [req.instructions.bool]
        [req.instructions.imperative])
  )


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






