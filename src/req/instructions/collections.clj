(ns req.instructions.collections)

(use '[req.interpreter]
     '[req.items])


(defn req-archive
  "conj's a copy of the entire collection onto itself"
  [coll]
  (if (interpreter? coll)
    (make-interpreter (req-archive (:queue coll)))
    (conj coll coll)))


(defn req-dup
  "two copies of a popped item are conj'ed onto the collection"
  [coll]
  (cond
    (interpreter? coll)
      (make-interpreter (req-dup (:queue coll)))
    (seq coll)
      (let [item (peek coll)]
        (conj (pop coll) item item))
    :else coll))


(defn req-empty
  "empties the collection of all items"
  [coll]
  (if (interpreter? coll)
    (make-interpreter (empty (:queue coll)))
    (empty coll)))


(defn req-next
  "moves the first item (leftmost) to the last position (tail), regardless of sequence type"
  [coll]
    (cond
      (interpreter? coll)
        (make-interpreter (req-next (:queue coll)))
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
      (make-interpreter (req-prev (:queue coll)))
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
        (make-interpreter (pop (:queue coll)))
      (seq coll)
        (list (peek coll) (pop coll))
      :else coll))




(defn req-reverse
  "returns a collection of the same type with the elements reversed"
  [coll]
    (cond
      (interpreter? coll)
        (make-interpreter (req-reverse (:queue coll)))
      (seq coll)
        (cond 
          (queue? coll) (new-queue (reverse coll))
          (list? coll) (reverse coll)
          (vector? coll) (into [] (reverse coll)))
      :else coll))


(defn req-swap
  "returns a collection with two items popped and conjed back in the reverse order"
  [coll]
    (cond
      (interpreter? coll)
        (make-interpreter (req-swap (:queue coll)))
      (seq coll)
        (let [a (peek coll)
              b (peek (pop coll))
              rest (pop (pop coll))]
          (conj rest a b))
      :else coll))


; req-shuffle?
; req-sort?
; req-filter?
; req-flatten?
; etc