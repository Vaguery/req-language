(ns req.imperative-test
  (:use midje.sweet)
  (:use [req.core]
        [req.interpreter]
        [req.items]
        [req.instructions.collections]))


(fact "an Imperative, when in the hot seat, applies its function to the running Interpreter"
  (let [quiet (->Imperative "noop" (fn [x] x))
        r (make-interpreter [quiet 1 2 3 4 5])]
    (readable-queue r) => ["‡noop‡" "1" "2" "3" "4" "5"]
    (:queue (step r)) => [1 2 3 4 5])

  (let [flipper (->Imperative "reverse" req-reverse)
        r (make-interpreter [flipper 1 2 3 4 5])]
    (readable-queue r) => ["‡reverse‡" "1" "2" "3" "4" "5"]
    (:queue (step r)) => [5 4 3 2 1])

  (let [dammit-archer (->Imperative "archive" req-archive)
        r (make-interpreter [dammit-archer 1 2 3 4 5])]
    (readable-queue r) => ["‡archive‡" "1" "2" "3" "4" "5"]
    (:queue (step r)) => [1 2 3 4 5 [1 2 3 4 5]]
    (type (last (:queue (step r)))) => clojure.lang.PersistentQueue))


(fact "there is no built-in checking, at the moment, whether it's a good idea!"
    (let [do-not-do-this (->Imperative "+" (fn [x] (+ x)))
        r (make-interpreter [do-not-do-this 1 2 3 4 5])]
    (readable-queue r) => ["‡+‡" "1" "2" "3" "4" "5"]
    (:queue (step r)) => (throws Exception)))


(fact "an Imperative can be used to set a Channel value"
  (let [setter (->Imperative "setter" #(set-channel % "y" 88))
        r (make-interpreter [setter 2])]
    (readable-queue r) => ["‡setter‡" "2"]
    (:queue (step r)) => [2]
    (:channels (step r)) => {"y" 88}
    ))