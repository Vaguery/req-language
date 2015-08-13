(ns req.core)


(defn new-queue 
  "creates a new PersistentQueue populated with the specified collection"
  [contents] (reduce conj (clojure.lang.PersistentQueue/EMPTY) contents))

;; interpreters

(defrecord Interpreter [queue])

(defn new-interpreter
  "creates a new ReQinterpreter with the specified collection in its queue"
  [items] (Interpreter. (new-queue items)))

;; interpreter stepping

(defn step
  "pops the top item from a ReQinterpreter and updates the interpreter state"
  [req]
  (let [top (peek (:queue req))
        tail (pop (:queue req))]
  (assoc req :queue (conj tail top))
  
  ))
