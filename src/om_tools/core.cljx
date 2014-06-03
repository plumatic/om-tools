(ns om-tools.core
  "Tools for Om"
  (:require
   [om.core :as om]
   [plumbing.fnk.schema]
   [plumbing.core :as p #+cljs :include-macros #+cljs true]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Private

#+clj
(defn- maybe-split-first [pred s]
  (if (pred (first s))
    [(first s) (next s)]
    [nil s]))

#+clj
(defn add-component-protocols [forms]
  (let [protocols {'display-name `om/IDisplayName
                   'init-state `om/IInitState
                   'should-update `om/IShouldUpdate
                   'will-mount `om/IWillMount
                   'did-mount `om/IDidMount
                   'will-unmount `om/IWillUnmount
                   'will-update `om/IWillUpdate
                   'did-update `om/IDidUpdate
                   'will-receive-props `om/IWillReceiveProps
                   'render `om/IRender
                   'render-state `om/IRenderState}]
    (mapcat (fn [form]
              (if-let [protocol (when (seq? form) (protocols (first form)))]
                [protocol form]
                [form]))
            forms)))

(defn state-proxy
  "Returns an atom-like object for reading and writing Om component state"
  [owner]
  #+cljs
  (let [get-state #(om.core/get-state owner)]
    (reify
      IDeref
      (-deref [_]
        (get-state))
      IReset
      (-reset! [_ v]
        (om.core/set-state! owner v))
      ISwap
      (-swap! [s f]
        (-reset! s (f (get-state))))
      (-swap! [s f x]
        (-reset! s (f (get-state) x)))
      (-swap! [s f x y]
        (-reset! s (f (get-state) x y)))
      (-swap! [s f x y more]
        (-reset! s (apply f (get-state) x y more))))))

#+clj
(defn convenience-constructor [f]
  `(defn ~(symbol (str "->" (name f)))
     ([cursor#]
        (om.core/build ~f cursor#))
     ([cursor# m#]
        (om.core/build ~f cursor# m#))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

(defmacro component
  "Simple sugar for defining Om component by removing need to reify
  and name Om protocols. All of the methods from React Life Cycle
  Protocols defined in om.core are recognized. These include:

   display-name, init-state, should-update, will-mount, did-mount,
   will-unmount, did-update, will-receive-props, render, and
   render-state

   Everything inside body is within an implicit reify; any other non-Om protocols
   can be specified if needed."
  [& forms]
  `(reify ~@(add-component-protocols forms)))

(defmacro defcomponent
  "Defines a component using a fnk." ;; TODO
  [name & args]
  (let [[doc-string? args] (maybe-split-first string? args)
        [attr-map? more-args] (maybe-split-first map? args)
        [arglist & forms] args]
    `(do
       (defn ~name
         ~@(when doc-string? [doc-string?])
         ~@(when attr-map? [attr-map?])
         [data# owner#]
         ((p/fnk ~arglist (component ~@forms))
          {:data data# :owner owner# :state (state-proxy owner#)}))
       ~(convenience-constructor name))))
