(ns req.core)


(defn new-queue 
  "creates a new PersistentQueue populated with the specified collection"
  [contents] (reduce conj (clojure.lang.PersistentQueue/EMPTY) contents))

;; interpreters

(defrecord Interpreter [queue])

(defn req-with
  "creates a new ReQinterpreter with the specified collection in its queue"
  [items] (Interpreter. (new-queue items)))



;; some instructions

(defn req-archive
  "puts a copy of the entire queue at its tail"
  [req]
  (let [q (:queue req)]
    (assoc req :queue (concat q q)))
  )


(defn req-dup
  "puts a copy of the top item on the queue at the tail of the queue"
  [req]
  (let [q (:queue req)
        top (peek q)]
  (if (nil? top)
    req
    (assoc req :queue (conj q top)))
  ))


(defn req-flush
  "empties the queue"
  [req]
  (req-with [])
  )


(defn req-next
  "sends the top item to the tail of the queue"
  [req]
  (let [q (:queue req)
        top (peek q)]
  (if (nil? top)
    req
    (assoc req :queue (conj (pop q) top)))
  ))


(defn req-pop
  "throws away the top item on the queue"
  [req]
  (let [q (:queue req)
        top (peek q)]
  (if (nil? top)
    req
    (assoc req :queue (pop q)))
  ))


(defn req-prev
  "sends the tail item to the head of the queue"
  [req]
  (let [q (:queue req)
        top (peek q)]
  (if (nil? top)
    req
    (req-with (cons (last q) (drop-last q))))
  ))


(defn req-reverse
  "reverses the queue order"
  [req]
  (let [q (:queue req)]
    (assoc req :queue (reverse q)))
  )


(defn req-swap
  "pop the top two items and requeue them in swapped order"
  [req]
  (let [q (:queue req)]
    (if (< (count q) 2)
    req 
    (let [item-1 (peek q) item-2 (second q)]
      (assoc req :queue (conj (pop (pop q)) item-2 item-1))))
    ))


;; Qlosure objects


(defrecord Qlosure [token transitions]
  Object
  (toString [_] 
    (str "«" token "»")))


(defn get-wants
  "returns the :wants table from a Qlosure item"
  [qlosure]
  (get-in qlosure [:transitions :wants]))


(defn req-wants
  "determines whether one ReQ item wants another; that is,
  whether the first can use the second as an argument to a function"
  [actor target]
  (if-let [wants (get-wants actor)]
    (if-let [want (first (filter #((second %) target) wants))]
      (first want)
      false)
    false))


(defn can-interact?
  "determines whether either ReQ item wants the other; returns a boolean"
  [item1 item2]
  (boolean (or (req-wants item1 item2) (req-wants item2 item1))))


(defn all-interacting-items
  "walks through a collection and returns all things that
  either are wanted by or want the actor"
  [actor items]
    (filter #(can-interact? actor %) items)
    )


(defn do-not-interact?
  "determines whether either of two ReQ items wants the other (a convenience for use with `split-with`)"
  [a b]
  (not (can-interact? a b)))


(defn split-with-interaction
  "splits a collection and returns a vector of two parts:
  the first contains only things that _do not_ interact with the
  actor; the second begins with the first item that _does_ interact
  with the actor; if none of them interact with the actor, the first
  collection will be empty"
  [actor items]
  (split-with (partial do-not-interact? actor) items))

(defn get-transformation
  "returns the indexed transformation from a Qlosure item"
  [qlosure item]
  (let [which (req-wants qlosure item)]
    (which (get-in qlosure [:transitions :transformations]))
    ))

(defn req-consume
  "applies an (unchecked) transformation from the actor onto the item arg"
  [actor item]
  ((get-transformation actor item) item)
  )

;; interpreter stepping


(defn step
  "pops the top item from a ReQinterpreter and updates the interpreter state"
  [req]
  (let [top (peek (:queue req))
        tail (pop (:queue req))
        popped-state (req-with tail)]
    (condp = top
      :archive (req-archive popped-state)
      :dup (req-dup popped-state)
      :flush (req-flush popped-state)
      :next (req-next popped-state)
      :reverse (req-reverse popped-state)
      :swap (req-swap popped-state)
      :pop (req-pop popped-state)
      :prev (req-prev popped-state)
      (assoc req :queue (conj tail top)))
  ))