(ns om-tools.util
  "Common functions")

(defn maybe-split-first [pred s]
  (if (pred (first s))
    [(first s) (next s)]
    [nil s]))

(defn possibly-destructured?
  "Returns true if top-level key k is destructured or potentially
  acessible from alias in a fnk-style destructure, otherwise false"
  [k args]
  (boolean
   (or (some #{(symbol (name k)) :as} args)
       (some #(and (vector? %) (= k (first %))) args))))
