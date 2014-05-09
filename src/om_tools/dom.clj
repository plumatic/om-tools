(ns om-tools.dom
  (:refer-clojure :exclude [map meta time])
  (:use plumbing.core)
  (:require
   [clojure.string :as str]
   [cljs.core :as cljs]
   om.dom)
  (:import
   [cljs.tagged_literals JSValue]))

(defn camel-case [s]
  (str/replace
   s #"-(\w)"
   #(str/upper-case (second %1))))

(defn opt-alias [opt]
  (case opt
    :class :className
    :for :htmlFor
    opt))

(defn format-opt [opt]
  (-> opt
      opt-alias
      name
      camel-case
      keyword))

(defn literal? [form]
  (some #(% form) [map? vector? number? keyword? string?]))

(def form-tags
  '[input textarea option])

(defn el-ctor [tag]
  (if (some (partial = tag) form-tags)
    (symbol "om.dom" (name tag))
    (symbol "js" (str "React.DOM." (name tag)))))

(defn ^:private gen-om-dom-inline-fn [tag]
  `(defmacro ~tag [opts# & children#]
     (let [ctor# '~(el-ctor tag)]
       (if (literal? opts#)
         (let [children# (if (map? opts#) children# (cons opts# children#))
               opts# (when (map? opts#) (JSValue. (map-keys format-opt opts#)))]
           `(~ctor# ~opts# ~@(flatten children#)))
         `(om-tools.dom/el ~ctor# ~opts# '~children#)))))

(defmacro ^:private gen-om-dom-inline-fns []
  `(do
     ~@(clojure.core/map gen-om-dom-inline-fn (concat om.dom/tags form-tags))))

(gen-om-dom-inline-fns)
