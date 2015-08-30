(ns req.items)


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
  (= (class item) req.items.Immortal))


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
  "applies an (unchecked) transition from the actor onto the item arg; if the actor doesn't want the item, it returns a list of the two unchanged"
  [actor item]
  (if (req-wants actor item)
    ((get-transition actor item) item)
    (list actor item)))


(defn ordered-consume
  "if the first arg wants the second, it consumes it; otherwise the second
  consumes the first"
  [item1 item2]
  (if (req-wants item1 item2)
    (req-consume item1 item2)
    (req-consume item2 item1)))



;; the entire ReQ type system

(def req (->  (make-hierarchy)
              (derive ::int ::num)
              (derive ::float ::num)
              (derive ::bool ::thing)
              (derive ::num ::thing)
              (derive ::nullary ::thing)
              (derive req.items.Nullary ::nullary)
              (derive java.lang.Number ::num)
              (derive java.lang.Long ::int)
              (derive java.lang.Double ::float)
              (derive java.lang.Boolean ::bool)
              (derive java.math.BigDecimal ::float)
              (derive clojure.lang.BigInt ::int)
              (derive clojure.lang.PersistentVector ::vec)
              ))


(def req-matchers
  {
    ::int integer? ;; most specific
    ::num number?
    ::bool boolean?
    ::vec vector?
    ::thing some?})  ;; least specific


(defn req-int?
  "returns true if the item is a req-int or a subtype of that type"
  [item]
  (isa? req (class item) ::int))


(defn req-float?
  "returns true if the item is a req-float or a subtype of that type"
  [item]
  (isa? req (class item) ::float))


(defn req-num?
  "returns true if the item is a req-num or a subtype of that type"
  [item]
  (isa? req (class item) ::num))


(defn req-bool?
  "returns true if the item is a req-bool or a subtype of that type"
  [item]
  (isa? req (class item) ::bool))


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
