(ns om-tools.dom
  "DOM element constructors for React. Mirrors om.dom namespace"
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

(defn format-opt-val [opt-val]
  (if (map? opt-val)
    (clj->js opt-val)
    opt-val))

(defn format-opts
  "Returns JavaScript object for React DOM attributes from opts map"
  [opts]
  (->> opts
       (clojure.core/map
        (fn [[k v]] [(format-opt-key k) (format-opt-val v)]))
       (into {})
       clj->js))

(defn ^boolean literal? [form]
  (not (or (symbol? form)
           (list? form))))

(defn ^boolean possible-coll? [form]
  (or (coll? form)
      (symbol? form)
      (list? form)))

(def form-tags
  '[input textarea option])

(defn el-ctor [tag]
  (if (some (partial = tag) form-tags)
    (symbol "om.dom" (name tag))
    (symbol "js" (str "React.DOM." (name tag)))))

#+clj
(defn object? [x]
  (instance? JSValue x))

(defn element-args [opts children]
  (cond
   (nil? opts) [nil children]
   (map? opts) [(format-opts opts) children]
   (object? opts) [opts children]
   :else [nil (cons opts children)]))

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

#+cljs
(defn class-set [m]
  "Returns a string of keys with truthy values joined together by spaces,
   or returns nil when no truthy values."
  (when-let [ks (->> m (filter val) keys (clojure.core/map name) distinct seq)]
    (str/join " " ks)))
