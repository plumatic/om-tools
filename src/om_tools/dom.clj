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
  (not (or (symbol? form)
           (list? form))))

(def form-tags
  '[input textarea option])

(defn el-ctor [tag]
  (if (some (partial = tag) form-tags)
    (symbol "om.dom" (name tag))
    (symbol "js" (str "React.DOM." (name tag)))))

(defn valid-opts? [opts]
  (or (nil? opts) (map? opts)))

(defn ^:private gen-om-dom-inline-fn [tag]
  `(defmacro ~tag [opts# & children#]
     (let [ctor# '~(el-ctor tag)]
       (if (literal? opts#)
         (let [[opts# children#] (if (valid-opts? opts#)
                                   [(when opts# (JSValue. (map-keys format-opt opts#))) children#]
                                   [nil (cons opts# children#)])]
           `(apply ~ctor# ~opts# (flatten '~children#)))
         `(om-tools.dom/el ~ctor# ~opts# '~children#)))))

(defmacro ^:private gen-om-dom-inline-fns []
  `(do
     ~@(clojure.core/map gen-om-dom-inline-fn (concat om.dom/tags form-tags))))

(gen-om-dom-inline-fns)
