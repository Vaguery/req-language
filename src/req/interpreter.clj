(ns req.interpreter)

(use '[req.items])


(defn new-queue 
  "creates a new PersistentQueue populated with the specified collection"
  ([] (clojure.lang.PersistentQueue/EMPTY))
  ([contents] (into (clojure.lang.PersistentQueue/EMPTY) contents)))


(defn append-this-to-queue
  "adds one item to the tail of a queue; if it's a `sequential`, it is concatenated, otherwise it is pushed"
  [q new-item]
  (if (sequential? new-item)
    (new-queue (concat q new-item))
    (conj q new-item)))


(defn queue-from-items
  "creates a new queue from the `base` collection, and applies `append-this-to-queue` to add all the remaining items if needed"
  [base & more-items]
  (new-queue (reduce append-this-to-queue base more-items)))


(defrecord Interpreter [queue])


(defn req-with
  "creates a new ReQinterpreter with the specified collection in its queue"
  [items] (Interpreter. (new-queue items)))


(defn readable-queue
  "takes a ReQ interpreter and applies `map str` to its `:queue`"
  [req]
  (map str (:queue req)))


;; interpreter stepping


(defn step
  "pops the top item from a ReQinterpreter and updates the interpreter state"
  [req]
  (let [hot-seat (peek (:queue req))
        tail (pop (:queue req))
        popped-state (req-with tail)]
    (cond
      (nullary? hot-seat)
        (req-with (queue-from-items tail ((:function hot-seat))))
      (seq (all-interacting-items hot-seat tail))
        (let [parts (split-with-interaction hot-seat tail)
              filler (first parts)
              target (first (second parts))
              result (ordered-consume hot-seat target)
              remainder (into [] (drop 1 (second parts)))]
              (req-with 
                (queue-from-items remainder filler result)))
      :else
        (assoc req :queue (conj tail hot-seat)))))


(defn req-steps
  "steps a ReQinterpreter into its future (producing a lazy sequence)"
  [req]
  (cons req (lazy-seq (req-steps (step req)))))


(defn nth-step
  "produces the specified step (0-based count) of the specified ReQ interpreter, using a lazy sequence to build it forward"
  [req n]
  (nth (req-steps req) n))
