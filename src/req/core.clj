(ns req.core)

(use  '[req.interpreter]
      '[req.items]
      '[req.instructions.bool]
      '[req.instructions.imperative])


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

