(ns om-tools.mixin
  "Mixins for Om made easy"
  (:require
   [om.core :as om]
   #?(:clj [om-tools.util :as util])
   #?(:clj cljs.core))
  #?(:clj
     (:import
      [cljs.tagged_literals JSValue])))

#?(:clj
   (def ^:private mixin-methods
     {'display-name       :getDisplayName
      'init-state         :getInitialState
      'will-mount         :componentWillMount
      'did-mount          :componentDidMount
      'will-unmount       :componentWillUnmount
      'will-update        :componentWillUpdate
      'did-update         :componentDidUpdate
      'will-receive-props :componentWillReceiveProps}))

#?(:clj
   (do
     (defmulti mixin-method-body
       "Returns quoted form for a function in React mixin object. Takes the
    method-name and rest of body from the defmixin macro and dispatches
    on method-name. All defmixin methods are expected to take an
    additional first argument of `this`, so returned functions should
    take one less argument than what's given in method-body and call
    original with `this` as first argument."
       (fn [method orig-body] method))

     (defmethod mixin-method-body :default
       [method orig-body]
       (let [this-sym (gensym "this")
             args (map gensym (next (first orig-body)))]
         `(fn [~@args]
            (cljs.core/this-as ~this-sym
              ((fn ~@orig-body) ~@(cons this-sym args))))))

     (doseq [method ['should-update 'will-update]]
       (defmethod mixin-method-body method
         [method orig-body]
         (assert (= 3 (count (first orig-body)))
                 (str "Invalid mixin: " method " should have 3 arguments: owner, next-props, next-state"))
         (let [this-sym (gensym "this")
               next-props-sym (gensym "next-props")]
           `(fn [~next-props-sym next-state#]
              (cljs.core/this-as ~this-sym
                ((fn ~@orig-body)
                 ~this-sym
                 (om/get-props ~(JSValue. {:props next-props-sym :isOmComponent true}))
                 (om/get-state ~this-sym)))))))

     (defmethod mixin-method-body 'did-update
       [method orig-body]
       (assert (= 3 (count (first orig-body)))
               "Invalid mixin: did-update should have 3 arguments: owner, prev-props, prev-state")
       (let [this-sym (gensym "this")
             prev-props-sym (gensym "prev-props")]
         `(fn [~prev-props-sym next-state#]
            (cljs.core/this-as ~this-sym
              ((fn ~@orig-body)
               ~this-sym
               (om/get-props ~(JSValue. {:props prev-props-sym :isOmComponent true}))
               (or (cljs.core/aget ~this-sym "state" "__om_prev_state")
                   (om/get-state ~this-sym)))))))

     (defmethod mixin-method-body 'will-receive-props
       [method orig-body]
       (assert (= 2 (count (first orig-body)))
               "Invalid mixin: will-receive-props should have 2 arguments: owner, next-props")
       (let [this-sym (gensym "this")
             next-props-sym (gensym "next-props")]
         `(fn [~next-props-sym]
            (cljs.core/this-as ~this-sym
              ((fn ~@orig-body)
               ~this-sym
               (om/get-props ~(JSValue. {:props next-props-sym :isOmComponent true})))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; defmixin validation

#?(:clj
   (defn- valid-mixin-form? [[method-name args :as form]]
     (and (seq? form)
          (symbol? method-name)
          (vector? args)
          (<= 1 (count args)))))

#?(:clj
   (defn- assert-valid-mixin
     "Throws IllegalArgumentException if mixin malformed"
     [name body]
     (when-not (symbol? name)
       (throw (IllegalArgumentException. "Invalid mixin name")))
     (when-let [invalid-form (first (remove valid-mixin-form? body))]
       (throw (IllegalArgumentException. (str "Unexpected form in mixin body: " invalid-form))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public
#?(:clj
   (defmacro defmixin
     "Defines a React mixin object.

   The following lifecycle methods are supported:
    - display-name
    - will-mount
    - did-mount
    - will-unmount
    - will-update
    - did-update
    - will-receive-props

   Note: init-state lifecycle does not currently behave as expected. (see prismatic/om-tools#29)
   Note: advanced compilation can break for non-lifecycle methods (see prismatic/om-tools#28)

   All other methods defined will be mixed into the owner component.
   Mixin methods are accessible from the base component via (.) accessor on owner.

   All methods receive owner React component as first argument.

   Example:

    (defmixin my-mixin
      (will-mount [owner] ...)
      (will-unmount [owner] ...)
      (did-update [owner prev-props prev-state] ...))"
     {:arglists '([name doc-string? (lifecycle-method [this args*] body)+])
      :added "0.2.0"}
     [name & args]
     (let [[doc-string? body] (util/maybe-split-first string? args)]
       (assert-valid-mixin name body)
       (let [kvs  (map (fn [[method & method-body]]
                         [(or (get mixin-methods method)
                              (munge (str method)))
                          (mixin-method-body method method-body)])
                       body)]
         `(def ~name
            ~@(when doc-string? [doc-string?])
            ~(JSValue. (into {} kvs)))))))
