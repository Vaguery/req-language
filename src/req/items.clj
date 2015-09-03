(ns req.items)


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


(defn queue?
  "returns true only if the argument is a clojure.lang.PersistentQueue"
  [item]
  (= (type item) clojure.lang.PersistentQueue))


;; Interpreter items


(defrecord Interpreter [queue])


(defn req-with
  "creates a new ReQinterpreter with the specified collection in its queue"
  [items] (Interpreter. (new-queue items)))


(defn readable-queue
  "takes a ReQ interpreter and applies `map str` to its `:queue`"
  [req]
  (map str (:queue req)))


;; ReQ core type definitions and type-checking infrastructure


(defn boolean?
  "is the item _specifically_ the value `true` or the value `false`?"
  [item] (or (false? item) (true? item)))


;; Immortal items


(defrecord Immortal [value]
  Object
  (toString [_] 
    (str value "⥀")))


(defn immortal?
  "returns true if the argument is an Immortal record"
  [item]
  (instance? req.items.Immortal item))


(defn immortalize
  "returns an Immortal record with the specified item as its :value; if the item is already an Immortal, it returns that"
  [item]
  (if (immortal? item)
    item
    (->Immortal item)))


;; Channels


(defrecord Channel [id value]
  Object
  (toString [_] 
    (str "⬍" id "|" (or value "?") "⬍")))


(defn channel?
  "returns true if the argument is an Channel record"
  [item]
  (instance? req.items.Channel item))


(defn active-channel?
  "returns true if the argument is a Channel record and has a set value"
  [item]
  (and (channel? item) (some? (:value item))))


(defn silent-channel?
  "returns true if the argument is a Channel record with :value nil"
  [item]
  (and (channel? item) (nil? (:value item))))


(defn immortal-item?
  "returns true if the argument is any kind of immortal item: an Immortal record, a Channel, etc"
  [item]
  (or (instance? req.items.Immortal item)
      (instance? req.items.Channel item)))


;; Nullary items ("Qlosures with no arguments")


(defrecord Nullary [token function]
  Object
    (toString [_] (str "«" token "»")))


(defn make-nullary [token function]
  (->Nullary token function))


;; Qlosure objects


(defrecord Qlosure [token wants transitions type]
  Object
  (toString [_] 
    (str "«" token "»")))


(defn make-qlosure
  "convenience function to create a Qlosure record with keyword labeled arguments"
  [token & {:keys [wants transitions type]
    :or {wants {} transitions {} type ::thing}}]
  (->Qlosure token wants transitions type)
  )


(defn qlosure?
  "returns true if the argument is a Qlosure record"
  [item]
  (= (class item) req.items.Qlosure))


(defn get-wants
  "returns the :wants table from any ReQ item"
  [item]
  (cond
    (immortal? item) (get-wants (:value item))
    (active-channel? item) (get-wants (:value item))
    :else (:wants item)))


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
  "applies an (unchecked) transition from the actor onto the item arg; if the actor doesn't want the item, it returns a list of the two unchanged"
  [actor item]
  (if (req-wants actor item)
    (cond
      (and (immortal-item? actor) (immortal-item? item))
        (list ((get-transition (:value actor) (:value item)) (:value item)) actor item)
      (immortal-item? actor)
        (list ((get-transition (:value actor) item) item) actor)
      (immortal-item? item)
        (list ((get-transition actor (:value item)) (:value item)) item)
      :else ((get-transition actor item) item))
  (list actor item)))


(defn ordered-consume
  "if the first arg wants the second, it consumes it; otherwise the second
  consumes the first"
  [item1 item2]
  (if (req-wants item1 item2)
    (req-consume item1 item2)
    (req-consume item2 item1)))


;;
;; the entire ReQ type system
;;
;;

(def req (->  (make-hierarchy)
              (derive ::int ::num)
              (derive ::float ::num)
              (derive ::bool ::thing)
              (derive ::num ::thing)
              (derive ::nullary ::thing)
              (derive ::qlosure ::thing)
              (derive ::channel ::thing)
              (derive req.items.Nullary ::nullary)
              (derive req.items.Qlosure ::qlosure)
              (derive java.lang.Number ::num)
              (derive java.lang.Long ::int)
              (derive java.lang.Double ::float)
              (derive java.lang.Boolean ::bool)
              (derive java.math.BigDecimal ::float)
              (derive clojure.lang.BigInt ::int)
              (derive clojure.lang.PersistentVector ::vec)
              ))


(defn req-int?
  "returns true if the item is a req-int or a subtype of that type"
  [item]
  (isa? req (type item) ::int))


(defn req-float?
  "returns true if the item is a req-float or a subtype of that type"
  [item]
  (isa? req (type item) ::float))


(defn req-num?
  "returns true if the item is a req-num or a subtype of that type"
  [item]
  (isa? req (type item) ::num))


(defn req-bool?
  "returns true if the item is a req-bool or a subtype of that type"
  [item]
  (isa? req (type item) ::bool))


(defn nullary?
  "returns true if the item is a Nullary or a subtype of that type"
  [item]
  (isa? req (type item) ::nullary))


(defn req-vec?
  "returns true if the item is a req-vec or a subtype of that type; with an optional function as the second argument, checks whether the vector only contains which return `true` when checked"
  ([item]
    (isa? req (class item) ::vec))
  ([checker item]
    (and
      (req-vec? item)
      (every? checker item))))


(defn req-type
  "determines the basic req-type of an item, first asking what it thinks"
  [item]
  (cond
    (immortal? item) (req-type (:value item))
    (silent-channel? item) ::channel
    (active-channel? item) (req-type (:value item))
    (qlosure? item) (:type item)
    (nullary? item) ::nullary
    (req-int? item) ::int
    (req-float? item) ::float
    (req-num? item) ::num
    (req-bool? item) ::bool
    (req-vec? item) ::vec
    :else ::thing
    ))


(defn req-type?
  "returns true when the req-type is that if the requested item"
  [type item]
  (= (req-type item) type))


(def req-matchers
  {
    ::int #(isa? req (req-type %) ::int) ;; most specific
    ::num #(isa? req (req-type %) ::num)
    ::bool #(isa? req (req-type %) ::bool)
    ::vec #(isa? req (req-type %) ::vec)
    ::thing some?})  ;; least specific




;; specialized ReQ item constructors


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