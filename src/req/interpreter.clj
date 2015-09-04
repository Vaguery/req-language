(ns req.interpreter)

(use '[req.items])


;; interpreter stepping


(defn step
  "pops the top item from a ReQinterpreter and updates the interpreter state"
  [req]
  (let [hot-seat (peek (:queue req))
        tail (pop (:queue req))
        popped-state (make-interpreter tail)]
    (cond
      (imperative? hot-seat)
        ((:function hot-seat) popped-state)
      (nullary? hot-seat)
        (make-interpreter (queue-from-items tail ((:function hot-seat))))
      (seq (all-interacting-items hot-seat tail))
        (let [parts (split-with-interaction hot-seat tail)
              filler (first parts)
              target (first (second parts))
              result (ordered-consume hot-seat target)
              remainder (into [] (drop 1 (second parts)))]
              (make-interpreter 
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
