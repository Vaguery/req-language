(ns req.instructions.collections)

(use '[req.interpreter]
     '[req.items])


(defn req-archive
  "conj's a copy of the entire collection onto itself"
  [coll]
  (conj coll coll))


(defn req-dup
  "two copies of a popped item are conj'ed onto the collection"
  [coll]
  (if (seq coll)
    (let [item (peek coll)]
      (conj (pop coll) item item))
    coll))


(defn req-empty
  "empties the collection of all items"
  [coll]
  (empty coll)
  )


(defn req-next
  "conj's a popped item from a collection back onto it (which has no visible effect on vectors or lists, but rotates queues)"
  [coll]
  (if (seq coll)
    (conj (pop coll) (peek coll))
    coll))



(defn req-pop
  "returns a list of a popped item and the remainder of the collection"
  [coll]
  (if (seq coll)
    (let [item (peek coll)]
      (list item (pop coll))
      )
    coll))




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
