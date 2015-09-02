(ns req.instructions.collections)

(use '[req.interpreter]
     '[req.items])


; (defn req-archive
;   "puts a copy of the entire queue at its tail"
;   [req]
;   (let [q (:queue req)]
;     (assoc req :queue (concat q q)))
;   )


(defn req-dup
  "two copies of a popped item are conj'ed onto the collection"
  [coll]
  (if (seq coll)
    (let [item (peek coll)]
      (conj (pop coll) item item))
    coll))


; (defn req-flush
;   "empties the queue"
;   [req]
;   (req-with [])
;   )


; (defn req-next
;   "sends the top item to the tail of the queue"
;   [req]
;   (let [q (:queue req)
;         top (peek q)]
;   (if (nil? top)
;     req
;     (assoc req :queue (conj (pop q) top)))
;   ))


(defn req-pop
  "returns a list of a popped item and the remainder of the collection"
  [coll]
  (if (seq coll)
    (let [item (peek coll)]
      (list item (pop coll))
      )
    coll))


; (defn req-prev
;   "sends the tail item to the head of the queue"
;   [req]
;   (let [q (:queue req)
;         top (peek q)]
;   (if (nil? top)
;     req
;     (req-with (cons (last q) (drop-last q))))
;   ))


; (defn req-reverse
;   "reverses the queue order"
;   [req]
;   (let [q (:queue req)]
;     (assoc req :queue (reverse q)))
;   )


; (defn req-swap
;   "pop the top two items and requeue them in swapped order"
;   [req]
;   (let [q (:queue req)]
;     (if (< (count q) 2)
;     req 
;     (let [item-1 (peek q) item-2 (second q)]
;       (assoc req :queue (conj (pop (pop q)) item-2 item-1))))
;     ))

; (def req-imperatives
;   {
;     :archive req-archive
;     :dup     req-dup
;     :flush   req-flush
;     :next    req-next
;     :reverse req-reverse
;     :swap    req-swap
;     :pop     req-pop
;     :prev    req-prev
;   })
