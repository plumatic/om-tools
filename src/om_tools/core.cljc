(ns om-tools.core
  "Tools for Om"
  (:require
   [om.core :as om]
   [plumbing.fnk.schema]
   [plumbing.core :as p #?(:cljs :include-macros) #?(:cljs true)]
   [schema.core :as s #?(:cljs :include-macros) #?(:cljs true)]
   #?(:clj [cljs.tagged-literals :refer [->JSValue]])
   #?(:clj [om-tools.util :as util])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Private

#?(:clj
   (def ^:private component-protocols
     {'display-name       `om/IDisplayName
      'init-state         `om/IInitState
      'should-update      `om/IShouldUpdate
      'will-mount         `om/IWillMount
      'did-mount          `om/IDidMount
      'will-unmount       `om/IWillUnmount
      'will-update        `om/IWillUpdate
      'did-update         `om/IDidUpdate
      'will-receive-props `om/IWillReceiveProps
      'render             `om/IRender
      'render-state       `om/IRenderState}))

#?(:clj
   (defn partial-spec->spec-map
     "Returns a map from protocol-or-object symbol to seq of method
   implementations forms (for reify), given a partial-spec. A
   partial-spec is a seq of forms that would be given to reify, except
   that Om lifecycle protocols do not need to be explicitly included."
     [partial-spec]
     (first
      (reduce
       (fn [[specs protocol] form]
         (cond
           (symbol? form) [specs form]

           (seq? form)
           (let [method (first form)
                 om-protocol (component-protocols method)
                 protocol (or om-protocol protocol 'object)]
             [(update-in specs [protocol] (fnil conj []) form) (when-not om-protocol protocol)])

           :else
           (throw (Exception. (str "Unknown form in component spec: " form)))))
       [{} nil]
       partial-spec))))

#?(:clj
   (defn component-spec
     "Returns a seq of quoted forms to be used inside reify/defrecord.
   Handles making Om lifecycle protocols explicit from partial-spec and
   merging in default implementations when not present in partial-spec."
     ([partial-spec] (component-spec partial-spec nil))
     ([partial-spec default-spec-map]
      (mapcat
       (fn [[protocol methods]] (cons protocol methods))
       (merge default-spec-map
              (partial-spec->spec-map partial-spec))))))

#?(:clj
   (defn spec-map-defaults
     "Returns a map of protocol symbol to default implemenation"
     [name-sym]
     {`om/IDisplayName [`(~'display-name [~'_] ~(name name-sym))]}))

#?(:clj
   (defn convenience-constructor
     "Returns quoted form that defs a function which calls om.core/build
  on Om component constructor, f. Function will be named the same as
  component constructor, but prefixed with \"->\" (like defrecord
  factory function). If descriptor-sym is a symbol, it is merged into build
  options map as :descriptor"
     [f descriptor-sym]
     (let [map-sym (gensym "m")]
       `(defn ~(symbol (str "->" (name f)))
          ([cursor#]
           (om/build ~f cursor#
                     ~@(when (symbol? descriptor-sym)
                         [{:descriptor descriptor-sym}])))
          ([cursor# ~map-sym]
           (om/build ~f cursor#
                     ~(if (symbol? descriptor-sym)
                        `(merge {:descriptor ~descriptor-sym} ~map-sym)
                        map-sym)))))))
#?(:clj
   (defn mixin-descriptor
     "Returns quoted form that defs a descriptor with mixins for Om
  component constructor, f. Descriptor will be named same as component
  constructor, but suffixed with \"$descriptor\""
     [f mixins]
     (when (seq mixins)
       (let [descriptor-sym (symbol (str (name f) "$descriptor"))]
         [descriptor-sym
          `(def ~descriptor-sym
             (let [descriptor# (om/specify-state-methods!
                                (cljs.core/clj->js om/pure-methods))]
               (aset descriptor# "mixins" ~(->JSValue (vec mixins)))
               descriptor#))]))))

#?(:clj
   (defn component-option?
     "Returns true if quoted form matches pattern of component option
  within defcomponent(k) body, otherwise false. Options are seqs that
  start with keywords."
     [form]
     (and (seq? form) (keyword? (first form)))))

#?(:clj
   (defn separate-component-config
     "Returns tuple of [options-map other-forms] from forms within
  defcomponent(k) macro body. Options-map is a map of option keyword
  to sequence of values; other-forms is sequence of all other
  non-option forms."
     [forms]
     (let [[option-forms other-forms] ((juxt filter remove) component-option? forms)]
       [(into {} (map (juxt first rest) option-forms))
        other-forms])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; defcomponent/k validation

#?(:clj
   (defn valid-component-form? [form]
     (or (and (seq? form)
              (symbol? (first form))
              (vector? (second form))
              (<= 1 (count (second form))))
         (symbol? form))))

#?(:clj
   (defn assert-valid-component
     "Throws IllegalArgumentException if component body is malformed."
     [body]
     (when-let [invalid-form (first (remove valid-component-form? body))]
       (throw (IllegalArgumentException.
               (str "Unexpected form in body of component: " invalid-form))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

#?(:cljs
   (defn state-proxy
     "Returns an atom-like object for reading and writing Om component
   state.

   Note: Behavior may exactly not match atoms when deref'ing
   immediately following a reset!/swap! because Om processes state
   changes asynchronously in separate render phases."
     [owner]
     (when owner
       (let [get-state #(om/get-state owner)]
         (reify
           IDeref
           (-deref [_]
             (get-state))
           IReset
           (-reset! [_ v]
             (om/set-state! owner v))
           ISwap
           (-swap! [s f]
             (-reset! s (f (get-state))))
           (-swap! [s f x]
             (-reset! s (f (get-state) x)))
           (-swap! [s f x y]
             (-reset! s (f (get-state) x y)))
           (-swap! [s f x y more]
             (-reset! s (apply f (get-state) x y more))))))))

#?(:clj
   (defmacro component
     "Simple sugar for defining Om component by removing need to reify
  and name Om protocols. All of the methods from React Life Cycle
  Protocols defined in om.core are recognized. These include:
    - display-name
    - init-state
    - should-update
    - will-mount
    - did-mount
    - will-unmount
    - did-update
    - will-receive-props
    - render
    - render-state

   Method definitions take the form:
   (methodname [args*] body)

   Example:

   (component
     (did-mount [this] ...)
     (render [this] ...)

   Everything inside body is within an implicit reify; any other non-Om protocols
   can be specified if needed."
     [& body]
     (assert-valid-component body)
     `(reify ~@(component-spec body))))

#?(:clj
   (defmacro defcomponent
     "Defines a function that returns an om.core/IRender or om.core/IRenderState
   instance. Follows the same pattern as clojure.core/defn, except that
    - Arguments support schema-style typehints (see schema.core/defn for more details)
    - The body is a set of Om lifecycle method-implementations

   Example:
    (defcomponent widget [data owner]
      (did-mount [this] ...)
      (render [this] ...))

   The following lifecycle methods are supported:
    - display-name
    - init-state
    - should-update
    - will-mount
    - did-mount
    - will-unmount
    - did-update
    - will-receive-props
    - render
    - render-state

   Options:
    :mixins  One or more React mixin objects. A descriptor specifying these mixins is
             automatically generated and defined as component-name$descriptor.

   In addition, a factory function will be defined: ->component-name,to wrap a call to
   om.core/build. If :mixins option is used, the generated constructor is used by default.

   Gotchas and limitations:
    - A render or render-state method (not both) must be defined.
    - Unlike clojure.core/defn, multi-arity is not supported (must use 2 xor 3 arguments)"
     {:arglists '([name doc-string? attr-map? [data owner opts?] prepost-map? (option-keyword option-values*)* (lifecycle-method [this args*] body)+])}
     [name & args]
     (let [[doc-string? args] (util/maybe-split-first string? args)
           [attr-map? args] (util/maybe-split-first map? args)
           [arglist & args] args
           [prepost-map? body] (util/maybe-split-first map? args)
           [config body] (separate-component-config body)
           [descriptor-sym descriptor] (mixin-descriptor name (:mixins config))]
       `(do
          ~descriptor
          (s/defn ~name
            ~@(when doc-string? [doc-string?])
            ~@(when attr-map? [attr-map?])
            ~arglist
            ~@(when prepost-map? [prepost-map?])
            (reify ~@(component-spec body (spec-map-defaults name))))
          ~(convenience-constructor name descriptor-sym)))))

#?(:clj
   (defmacro defcomponentk
     "Defines a function that returns an om.core/IRender or om.core/IRenderState
   instance. Follows the same pattern as plumbing.core/defnk, except that
   the body is a set of Om lifecycle-method implementations.

   Refer to (doc defcomponent) for supported lifecycle-methods and
   component options.

   The arguments receive a map with the following keys:
    :data   The data (cursor) passed to component when built
    :owner  The backing React component
    :opts   The optional map of options passed when built
    :shared The map of globally shared values
    :state  An atom-like object for convenience to om.core/get-state and
            om.core/set-state!

   In addition, a factory function will be defined: ->component-name,
   to wrap a call to om.core/build, providing any defaults.

   Example:

    (defcomponent list-of-gadgets [[:data gadgets] state]
      (did-mount [_] ...)
      (render [_] ...))"
     {:arglists '([name doc-string? attr-map? [bindings*] prepost-map? (option-keyword option-values*)* (lifecycle-method [this args*] body)+])}
     [name & args]
     (let [[doc-string? args] (util/maybe-split-first string? args)
           [attr-map? args] (util/maybe-split-first map? args)
           [arglist & args] args
           [prepost-map? body] (util/maybe-split-first map? args)
           [config body] (separate-component-config body)
           [descriptor-sym descriptor] (mixin-descriptor name (:mixins config))
           owner-sym (gensym "owner")
           constructor (with-meta (gensym 'constructor)
                         (select-keys (meta name) [:always-validate :never-validate]))]
       `(do
          ~descriptor
          (let [component-fnk# (p/fnk ~constructor ~arglist
                                      ~@(when prepost-map? [prepost-map?])
                                      (reify ~@(component-spec body (spec-map-defaults name))))]
            (defn ~name
              ~@(when doc-string? [doc-string?])
              ~@(when attr-map? [attr-map?])
              [data# ~owner-sym & [opts#]]
              (component-fnk#
               {:data data#
                :owner ~owner-sym
                :opts opts#
                ~@(when (util/possibly-destructured? :shared arglist)
                    [:shared `(om/get-shared ~owner-sym)])
                ~@(when (util/possibly-destructured? :state arglist)
                    [:state `(state-proxy ~owner-sym)])})))
          ~(convenience-constructor name descriptor-sym)))))

#?(:clj
   (defmacro defcomponentmethod
     "Like clojure.core/defmethod, but body is parsed like defcomponent.

   Multimethod components can be used to dynamically mount different components definitions
   under same component name.

   The signature of the multimethod's dispatch function should same as Om component
   constructor. Arguments support schema-style typehints (see schema.core/defn
   for more details)

   Refer to (doc defcomponent) for supported lifecycle-methods and
   component options.

   Does not support :mixins option"
     [multifn dispatch-val & fn-tail]
     (let [[fn-name? [args & body]] (util/maybe-split-first symbol? fn-tail)
           [config body] (separate-component-config body)]
       (assert (vector? args) "Parameter declaration should be a vector")
       (assert (not (:mixins config)) "Mixins are not suppoted in multi-component")
       `(s/defmethod ~multifn ~dispatch-val
          ~@(when fn-name? [fn-name?])
          ~args
          (reify ~@(component-spec body))))))

#?(:cljs
   (defn set-state?!
     "Calls om.core/set-state! when current value not= to v and returns
   updated owner, otherwise nil.
   Used to prevent no-op updates from entering render queue"
     {:added "0.2.0"}
     ([owner v]
      (when-not (= v (om/get-state owner))
        (om/set-state! owner v)))
     ([owner korks v]
      (when-not (= v (om/get-state owner korks))
        (om/set-state! owner korks v)))))
