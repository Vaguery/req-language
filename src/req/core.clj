(ns req.core)

(defn empty-queue [] (clojure.lang.PersistentQueue/EMPTY)) 

(defn new-queue 
  "creates a new PersistentQueue populated with the argument elements"
  [contents]
  (reduce conj (clojure.lang.PersistentQueue/EMPTY) contents))