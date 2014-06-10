(ns om-tools.core
  "Tools for Om"
  #+cljs
  (:require-macros
   [schema.macros :as sm])
  (:require
   [om.core :as om]
   [plumbing.fnk.schema]
   [plumbing.core :as p #+cljs :include-macros #+cljs true]
   #+clj [om-tools.util :as util]
   #+clj [schema.macros :as sm]
   #+clj cljs.core)
  #+clj
  (:import
   [cljs.tagged_literals JSValue]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Private

#+clj
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
   'render-state       `om/IRenderState})

#+clj
(defn add-component-protocols [forms]
  (mapcat
   (fn [form]
     (if-let [protocol (when (seq? form) (component-protocols (first form)))]
       [protocol form]
       [form]))
   forms))

#+clj
(defn convenience-constructor [f ctor-sym]
  (let [map-sym (gensym "m")]
    `(defn ~(symbol (str "->" (name f)))
       ([cursor#]
          (om/build ~f cursor#
                    ~@(when ctor-sym [{:ctor ctor-sym}])))
       ([cursor# ~map-sym]
          (om/build ~f cursor#
                    ~(if ctor-sym
                       `(merge {:ctor ~ctor-sym} ~map-sym)
                       map-sym))))))
#+clj
(defn mixin-constructor [f mixins]
  (when (seq mixins)
    (let [ctor-sym (symbol (str (name f) "$ctor"))]
      [ctor-sym
       `(def ~ctor-sym
          (let [obj# (om/specify-state-methods! (cljs.core/clj->js om/pure-methods))]
            (aset obj# "mixins" ~(JSValue. (vec mixins)))
            (. js/React (~'createClass obj#))))])))

#+clj
(defn separate-component-config [forms]
  (let [opt? #(and (seq? %) (keyword? (first %)))]
    [(->> (filter opt? forms) (map (juxt first rest)) (into {}))
     (remove opt? forms)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

#+cljs
(defn state-proxy
  "Returns an atom-like object for reading and writing Om component state"
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
          (-reset! s (apply f (get-state) x y more)))))))

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
  `(reify ~@(add-component-protocols body)))

(defmacro defcomponent
  "Defines a function that returns an om.core/IRender or om.core/IRenderState
   instance. Follows the same pattern as clojure.core/defn, except that
    - Arguments support schema-style typehints (see schema.macros/defn for more details)
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
    :mixins  One or more React mixin objects. A constructor specifying these mixins is
             automatically generated and defined as component-name$ctor.

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
        [ctor-sym ctor-fn] (mixin-constructor name (:mixins config))]
    `(do
       ~ctor-fn
       (sm/defn ~name
         ~@(when doc-string? [doc-string?])
         ~@(when attr-map? [attr-map?])
         ~arglist
         ~@(when prepost-map? [prepost-map?])
         (component ~@body))
       ~(convenience-constructor name ctor-sym))))

(defmacro defcomponentk
  "Defines a function that returns an om.core/IRender or om.core/IRenderState
   instance. Follows the same pattern as plumbing.core/defnk, except that
   the body is a set of Om lifecycle-method implementations.

   Refer to defcomponent doc-string for supported lifecycle-methods and
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
        [ctor-sym ctor-fn] (mixin-constructor name (:mixins config))
        owner-sym (gensym "owner")]
    `(do
       ~ctor-fn
       (defn ~name
         ~@(when doc-string? [doc-string?])
         ~@(when attr-map? [attr-map?])
         [data# ~owner-sym & [opts#]]
         ((p/fnk
            ~arglist
            ~@(when prepost-map? [prepost-map?])
            (component ~@body))
          {:data data#
           :owner ~owner-sym
           :opts opts#
           ~@(when (util/possibly-destructured? :shared arglist)
               [:shared `(om/get-shared ~owner-sym)])
           ~@(when (util/possibly-destructured? :state arglist)
               [:state `(state-proxy ~owner-sym)])}))
       ~(convenience-constructor name ctor-sym))))

#+cljs
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
       (om/set-state! owner korks v))))
