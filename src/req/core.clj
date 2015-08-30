(ns req.core)


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


;; interpreters

(defrecord Interpreter [queue])


(defn req-with
  "creates a new ReQinterpreter with the specified collection in its queue"
  [items] (Interpreter. (new-queue items)))


(defn readable-queue
  "takes a ReQ interpreter and applies `map str` to its `:queue`"
  [req]
  (map str (:queue req))) 

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

;; Immortal items

(defrecord Immortal [value]
  Object
  (toString [_] 
    (str value "⥀")))


(defn immortal?
  "returns true if the argument is an Immortal record"
  [item]
  (= (class item) req.core.Immortal))

;; ReQ type checking infrastructure


(defn boolean?
  "is the item _specifically_ the value `true` or the value `false`?"
  [item] (or (false? item) (true? item)))



;; ReQ type system

(def req (->  (make-hierarchy)
              (derive ::int ::num)
              (derive ::float ::num)
              (derive ::bool ::thing)
              (derive ::num ::thing)
              (derive java.lang.Number ::num)
              (derive java.lang.Long ::int)
              (derive java.lang.Double ::float)
              (derive java.lang.Boolean ::bool)
              (derive java.math.BigDecimal ::float)
              (derive clojure.lang.BigInt ::int)
              ))


(def req-matchers
  {
    ::int integer? ;; most specific
    ::num number?
    ::bool boolean?
    ::vec vector?
    ::thing some?})  ;; least specific


(defn req-type
  "determines the basic req-type of an item, first asking what it thinks"
  [item]
  (cond
    (immortal? item) (req-type (:value item))
    (isa? req (class item) ::int) ::int
    (isa? req (class item) ::float) ::float
    (isa? req (class item) ::num) ::num
    (isa? req (class item) ::bool) ::bool
    (vector? item) ::vec
    :else ::thing
    ))

(defn req-type?
  "returns true when the req-type is that if the requested item"
  [type item]
  (= (req-type item) type))


;; Qlosure objects


(defrecord Qlosure [token wants transitions]
  Object
  (toString [_] 
    (str "«" token "»")))


(defn make-qlosure
  "convenience function to create a Qlosure record with keyword labeled arguments"
  [token & {:keys [wants transitions] :or {wants {} transitions {}}}]
  (->Qlosure token wants transitions)
  )


(defn get-wants
  "returns the :wants table from a Qlosure item"
  [qlosure]
  (:wants qlosure))


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


(defn get-transition
  "returns the indexed transition from a Qlosure item"
  [qlosure item]
  (let [which (req-wants qlosure item)]
    (which (:transitions qlosure))
    ))


(defn req-consume
  "applies an (unchecked) transition from the actor onto the item arg"
  [actor item]
  ((get-transition actor item) item)
  )


(defn ordered-consume
  "if the first arg wants the second, it consumes it; otherwise the second
  consumes the first"
  [item1 item2]
  (if (req-wants item1 item2)
    (req-consume item1 item2)
    (req-consume item2 item1)))

;; Nullary items ("Qlosures with no arguments")

(defrecord Nullary [token function]
  Object
    (toString [_] (str "«" token "»")))

(defn make-nullary [token function]
  (->Nullary token function))

;; interpreter stepping


(def req-imperatives
  {
    :archive req-archive
    :dup     req-dup
    :flush   req-flush
    :next    req-next
    :reverse req-reverse
    :swap    req-swap
    :pop     req-pop
    :prev    req-prev
  })


(defn step
  "pops the top item from a ReQinterpreter and updates the interpreter state"
  [req]
  (let [hot (peek (:queue req))
        tail (pop (:queue req))
        popped-state (req-with tail)]
    (cond
      (= (type hot) Nullary)
        (req-with (queue-from-items tail ((:function hot))))
      (contains? req-imperatives hot)
        ((hot req-imperatives) popped-state)
      (seq (all-interacting-items hot tail))
        (let [parts (split-with-interaction hot tail)
              filler (first parts)
              target (first (second parts))
              result (ordered-consume hot target)
              remainder (into [] (drop 1 (second parts)))]
              (req-with 
                (queue-from-items remainder filler result)))
      :else
        (assoc req :queue (conj tail hot)))
  ))

(defn req-steps
  "steps a ReQinterpreter into its future (producing a lazy sequence)"
  [req]
  (cons req (lazy-seq (req-steps (step req))))
  )

(defn nth-step
  [req n]
  (nth (req-steps req) n))


;; specialized constructors

; TODO make this work with req-type?
; make work with isa?

(defn make-binary-one-type-qlosure
  "produces a Qlosure with two arguments of the same specified type"
  [token type-kw operator]
  (let [match-pair {type-kw (type-kw req-matchers)}]
  (make-qlosure
    token   
    :wants match-pair
    :transitions {type-kw 
      (fn [item]
        (make-qlosure
          (str item token "⦿")
          :wants match-pair
          :transitions {type-kw (partial operator item)}))
       }
  )))

;; TODO make this work with req-type?

(defn make-unary-qlosure
  "produces a Qlosure with one argument of the specified type"
  [token type-kw operator]
  (let [match-pair {type-kw (type-kw req-matchers)}]
  (make-qlosure
    token   
    :wants match-pair
    :transitions {type-kw (partial operator)})))


(defn make-seed
  "creates a Nullary which emits its contents and persists"
  [token contents]
  (make-nullary
    token
    (fn [] (list (make-seed token contents) contents))))


(defn make-timer
  "creates a Nullary which counts from the first integer to the end; if there is a payload, it emits that every step"
  ([state end]
    (make-nullary
      (str "timer:" state "-" end)
      (fn [] 
        (if (>= (inc state) end)
          end
          (make-timer (inc state) end)))))
  ([state end contents]
    (make-nullary
      (str "timer:" state "-" end)
      (fn [] 
        (if (>= (inc state) end)
        end
        (list (make-timer (inc state) end contents) contents))))))


(defn make-looper
  "creates a Nullary looper from a collection; cycles the collection and emits the top item each step"
  [collection]
    (make-nullary
      (str collection)
      (fn [] 
        (list 
          (make-looper (into [] (concat (drop 1 collection) (take 1 collection))))
          (first collection))
  )))

;; boolean functions (to be moved to a new file eventually)

(defn ∧
  "badly-conceived pure 'boolean' binary logical AND function"
  [arg1 arg2]
  (and arg1 arg2)
  )

(defn ∨
  "badly-conceived pure 'boolean' binary logical OR function"
  [arg1 arg2]
  (or arg1 arg2)
  )
