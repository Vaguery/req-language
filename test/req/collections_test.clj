(ns req.collections-test
  (:use midje.sweet)
  (:use [req.core]
        [req.interpreter]
        [req.items]
        [req.instructions.collections])
  )

;; collection instructions

(fact "when req-dup is executed, two copies of a popped item are conj'ed onto the collection"
  (req-dup '(1 2 3 4)) => '(1 1 2 3 4)
  (req-dup [1 2 3 4]) => [1 2 3 4 4]
  (req-dup (new-queue [1 2 3 4])) => [2 3 4 1 1]
  )


(fact "req-dup returns the collection if it's empty"
  (req-dup '()) => '()
  (req-dup []) => []
  (req-dup (new-queue)) => []
)


(fact "req-dup works on Interpreter instances"
  (:queue (req-dup (req-with [1 2 3 4]))) => [2 3 4 1 1]
  )


(fact "when req-pop is executed, a list holding a popped item and the remainder of the collection is returned"
  (req-pop '(1 2 3 4)) => '( 1 (2 3 4))
  (req-pop [1 2 3 4]) => '(4 [1 2 3])
  (req-pop (new-queue [1 2 3 4])) => '(1 (2 3 4))
  (class (second (req-pop (new-queue [1 2 3 4])))) => clojure.lang.PersistentQueue
  )


(fact "req-pop returns the collection if it's empty"
  (req-pop '()) => '()
  (req-pop []) => []
  (req-pop (new-queue)) => []
)


(fact "req-pop works on Interpreter instances, but discards the popped item"
  (:queue (req-pop (req-with [1 2 3 4]))) => [2 3 4]
  )


(fact "when req-archive is executed, the entire collection is conj'ed onto itself"
  (req-archive '(1 2 3 4)) => '((1 2 3 4) 1 2 3 4)
  (req-archive [1 2 3 4]) => '[1 2 3 4 [1 2 3 4]]
  (req-archive (new-queue [1 2 3 4])) => '(1 2 3 4 (1 2 3 4))
  (class (last (req-archive (new-queue [1 2 3 4])))) => clojure.lang.PersistentQueue
  )


(fact "req-archive puts an empty copy into the collection even if it was empty"
  (req-archive '()) => '(())
  (req-archive []) => [[]]
  (req-archive (new-queue)) => [[]]
)


(fact "req-archive works on Interpreter instances"
  (:queue (req-archive (req-with [1 2 3 4]))) => [1 2 3 4 [1 2 3 4]]
  )



(fact "when req-empty is executed, the entire collection is emptied"
  (req-empty '(1 2 3 4)) => '()
  (req-empty [1 2 3 4]) => []
  (req-empty (new-queue [1 2 3 4])) => '()
  (class (req-empty (new-queue [1 2 3 4]))) => clojure.lang.PersistentQueue
  )


(fact "req-empty works on empty collections, too"
  (req-empty '()) => '()
  (req-empty []) => []
  (req-empty (new-queue)) => []
)


(fact "req-empty works on Interpreter instances"
  (:queue (req-empty (req-with [1 2 3 4]))) => []
  )


(fact "when req-next is executed, the left item moves to the right end"
  (req-next '(1 2 3 4)) => '(2 3 4 1)
  (list? (req-next '(1 2 3 4))) => true

  (req-next [1 2 3 4]) => [2 3 4 1]
  (vector? (req-next [1 2 3 4])) => true

  (req-next (new-queue [1 2 3 4])) => [2 3 4 1]
  (queue? (new-queue [1 2 3 4])) => true
  (queue? (req-next (new-queue [1 2 3 4]))) => true
  )


(fact "req-next returns the collection if it's empty"
  (req-next '()) => '()
  (req-next []) => []
  (req-next (new-queue)) => []
)


(fact "req-next works on Interpreter instances"
  (:queue (req-next (req-with [1 2 3 4]))) => [2 3 4 1]
  )


(fact "when req-prev is executed, the rightmost item moves to the left end"
  (req-prev '(1 2 3 4)) => '(4 1 2 3)
  (list? (req-prev '(1 2 3 4))) => true

  (req-prev [1 2 3 4]) => [4 1 2 3]
  (vector? (req-prev [1 2 3 4])) => true
  
  (req-prev (new-queue [1 2 3 4])) => [4 1 2 3]
  (queue? (new-queue [1 2 3 4])) => true
  (queue? (req-prev (new-queue [1 2 3 4]))) => true
  )


(fact "req-prev returns the collection if it's empty"
  (req-prev '()) => '()
  (req-prev []) => []
  (req-prev (new-queue)) => []
)


(fact "req-prev works on Interpreter instances"
  (:queue (req-prev (req-with [1 2 3 4]))) => [4 1 2 3]
  )
