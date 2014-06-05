(ns om-tools.dom
  (:refer-clojure :exclude [map meta time])
  (:require
   [clojure.string :as str]
   om.dom
   #+clj cljs.core)
  #+clj
  (:import
   [cljs.tagged_literals JSValue]))

#+clj
(defn clj->js [v]
  (JSValue. v))

(defn map-keys [f m]
  (into {} (for [[k v] m]
             [(f k) v])))

(defn camel-case [s]
  (str/replace
   s #"-(\w)"
   #(str/upper-case (second %))))

(defn opt-key-alias [opt]
  (case opt
    :class :className
    :for :htmlFor
    opt))

(defn format-opt-key [opt-key]
  (-> opt-key
      opt-key-alias
      name
      camel-case
      keyword))

(defn format-opt-style [opt]
  (let [[k v] opt]
    (if (= k :style)
      [k (clj->js v)]
      opt)))

(defn format-opts [opts]
  (->> opts
       (map-keys format-opt-key)
       (clojure.core/map format-opt-style)
       (into {})
       clj->js))

(defn literal? [form]
  (not (or (symbol? form)
           (list? form))))

(defn possible-coll? [form]
  (or (coll? form)
      (symbol? form)
      (list? form)))

(def form-tags
  '[input textarea option])

(defn el-ctor [tag]
  (if (some (partial = tag) form-tags)
    (symbol "om.dom" (name tag))
    (symbol "js" (str "React.DOM." (name tag)))))

(defn valid-opts? [opts]
  (or (nil? opts) (map? opts)))

(defn element-args [opts children]
  (if (valid-opts? opts)
    [(when opts (format-opts opts)) children]
    [nil (cons opts children)]))

#+cljs
(defn element [ctor opts children]
  (let [[opts children] (element-args opts children)]
    (apply ctor (flatten (cons opts children)))))

#+clj
(defn ^:private gen-om-dom-inline-fn [tag]
  `(defmacro ~tag [opts# & children#]
     (let [ctor# '~(el-ctor tag)]
       (if (literal? opts#)
         (let [[opts# children#] (element-args opts# children#)]
           (cond
            (every? (complement possible-coll?) children#)
            `(~ctor# ~opts# ~@children#)

            (and (= (count children#) 1) (vector? (first children#)))
            `(~ctor# ~opts# ~@(-> children# first flatten))

            :else
            `(apply ~ctor# ~opts# (flatten (vector ~@children#)))))
         `(om-tools.dom/element ~ctor# ~opts# (vector ~@children#))))))

(defmacro ^:private gen-om-dom-inline-fns []
  `(do
     ~@(clojure.core/map gen-om-dom-inline-fn (concat om.dom/tags form-tags))))

#+clj
(gen-om-dom-inline-fns)
