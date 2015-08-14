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

