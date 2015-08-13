(ns req.core)


(defn new-queue 
  "creates a new PersistentQueue populated with the specified collection"
  [contents] (reduce conj (clojure.lang.PersistentQueue/EMPTY) contents))

;; interpreters

(defrecord Interpreter [queue])

(defn new-interpreter
  "creates a new ReQinterpreter with the specified collection in its queue"
  [items] (atom (Interpreter. (new-queue items))))