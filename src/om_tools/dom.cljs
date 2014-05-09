(ns om-tools.dom
  (:require om.dom))

(defn valid-opts? [opts]
  (or (nil? opts) (map? opts)))

(defn el [tag-fn opts children]
  (let [[opts children] (if (valid-opts? opts)
                          [(clj->js opts) children]
                          [nil (cons opts children)])]
    (apply tag-fn (flatten (cons opts children)))))
