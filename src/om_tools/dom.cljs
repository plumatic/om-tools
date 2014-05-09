(ns om-tools.dom
  (:require om.dom))

(defn el [tag-fn opts children]
  (let [children (to-array children)
        opts (clj->js opts)]
    (.unshift children opts)
    (apply tag-fn children)))
