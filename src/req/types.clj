(ns req.types)


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
  (= (class item) req.types.Immortal))


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
  (= (class item) req.types.Qlosure))


(defn get-wants
  "returns the :wants table from a Qlosure item"
  [qlosure]
  (:wants qlosure))



;; the entire ReQ type system

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