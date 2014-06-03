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
  ""
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
          {:data data# :owner owner#}))
       ~(convenience-constructor name))))
