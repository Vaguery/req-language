(ns req.instructions.collections)

(use '[req.interpreter]
     '[req.items])


(defn req-archive
  "conj's a copy of the entire collection onto itself"
  [coll]
  (if (interpreter? coll)
    (req-with (req-archive (:queue coll)))
    (conj coll coll)))


(defn req-dup
  "two copies of a popped item are conj'ed onto the collection"
  [coll]
  (cond
    (interpreter? coll)
      (req-with (req-dup (:queue coll)))
    (seq coll)
      (let [item (peek coll)]
        (conj (pop coll) item item))
    :else coll))


(defn req-empty
  "empties the collection of all items"
  [coll]
  (if (interpreter? coll)
    (req-with (empty (:queue coll)))
    (empty coll)))


(defn req-next
  "moves the first item (leftmost) to the last position (tail), regardless of sequence type"
  [coll]
    (cond
      (interpreter? coll)
        (req-with (req-next (:queue coll)))
      (seq coll)
        (cond 
          (queue? coll) (conj (pop coll) (peek coll))
          (list? coll) (reverse (into '() (concat (rest coll) (list (first coll)))))
          (vector? coll) (vec (concat (rest coll) (list (first coll)))))
      :else coll))


(defn req-prev
  "moves the last item (rightmost) to the first position (left), regardless of sequence type"
  [coll]
  (cond
    (interpreter? coll)
      (req-with (req-prev (:queue coll)))
    (seq coll)
      (cond 
        (queue? coll) (new-queue (cons (last coll) (butlast coll)))
        (list? coll) (reverse (into '() (cons (last coll) (butlast coll))))
        (vector? coll) (into [] (cons (last coll) (butlast coll))))
    :else coll))



(defn req-pop
  "returns a list of a popped item and the remainder of the collection"
  [coll]
    (cond
    (interpreter? coll)
      (req-with (pop (:queue coll)))
    (seq coll)
      (list (peek coll) (pop coll))
    :else coll))




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
