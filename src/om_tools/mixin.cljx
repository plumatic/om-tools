(ns om-tools.mixin
  "Mixins for Om made easy"
  (:require
   [om.core :as om]
   #+clj [om-tools.util :as util]
   #+clj [schema.macros :as sm]
   #+clj cljs.core)
  #+clj
  (:import
   [cljs.tagged_literals JSValue]))

#+clj
(def ^:private mixin-methods
  {'display-name       :getDisplayName
   'init-state         :getInitialState
   'will-mount         :componentWillMount
   'did-mount          :componentDidMount
   'will-unmount       :componentWillUnmount
   'will-update        :componentWillUpdate
   'did-update         :componentDidUpdate
   'will-receive-props :componentWillReceiveProps})

#+clj
(do
  (defmulti mixin-method-body (fn [method orig-body] method))

  (defmethod mixin-method-body :default
    [method orig-body]
    (let [this-sym (gensym "this")
          args (map gensym (first orig-body))]
      `(fn [~@args]
         (cljs.core/this-as ~this-sym
           ((fn ~@orig-body) ~@(cons this-sym args))))))

  (doseq [method ['should-update 'will-update]]
    (defmethod mixin-method-body method
      [method orig-body]
      (let [this-sym (gensym "this")
            next-props-sym (gensym "next-props")]
        `(fn [~next-props-sym next-state#]
           (cljs.core/this-as ~this-sym
             ((fn ~@orig-body)
              ~this-sym
              (om/get-props ~(JSValue. {:props next-props-sym}))
              (om/get-state ~this-sym)))))))

  (defmethod mixin-method-body 'did-update
    [method orig-body]
    (let [this-sym (gensym "this")
          prev-props-sym (gensym "prev-props")]
      `(fn [~prev-props-sym next-state#]
         (cljs.core/this-as ~this-sym
           ((fn ~@orig-body)
            ~this-sym
            (om/get-props ~(JSValue. {:props prev-props-sym}))
            (or (cljs.core/aget ~this-sym "state" "__om_prev_state")
                (om/get-state ~this-sym)))))))

  (defmethod mixin-method-body 'will-receive-props
    [method orig-body]
    (let [this-sym (gensym "this")
          next-props-sym (gensym "next-props")]
      `(fn [~next-props-sym]
         (cljs.core/this-as ~this-sym
           ((fn ~@orig-body)
            ~this-sym
            (om/get-props ~(JSValue. {:props next-props-sym}))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

(defmacro defmixin
  "Defines a React mixin object.

   The following lifecycle methods are supported:
    - display-name
    - init-state
    - will-mount
    - did-mount
    - will-unmount
    - will-update
    - did-update
    - will-receive-props

   Example:

    (defmixin my-mixin
      (will-mount [owner] ...)
      (will-unmount [owner] ...)
      (did-update [owner prev-props prev-state] ...))"
  {:arglists '([name doc-string? (lifecycle-method [this args*] body)+])
   :added "0.2.0"}
  [name & args]
  (let [[doc-string? body] (util/maybe-split-first string? args)]
    (assert (every? seq? body) "Invalid mixin form")
    (assert (every? #(and (>= (count %) 2) (vector? (second %))) body) "Invalid mixin method form")
    (let [kvs  (map (fn [[method & method-body]]
                      (assert (contains? mixin-methods method) (str "Invalid mixin method: " method))
                      [(get mixin-methods method)
                       (mixin-method-body method method-body)])
                    body)]
      `(def ~name
         ~@(when doc-string? [doc-string?])
         ~(JSValue. (into {} kvs))))))
