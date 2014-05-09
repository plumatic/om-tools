(ns om-tools.core
  "Tools for Om"
  (:require
   [om.core :as om]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Private

(defn- maybe-split-first [pred s]
  (if (pred (first s))
    [(first s) (next s)]
    [nil s]))

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
              (if-let [protocol (when (seq form) (protocols (first form)))]
                [protocol form]
                [form]))
            forms)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

(defmacro defcomponent
  "Very simple sugar for defining Om components by removing need to refiy and
   name Om protocols. All of the methods from React Life Cycle Protocols defined
   in om.core are recognized. These include:

   display-name, init-state, should-update, will-mount, did-mount, will-unmount,
   did-update, will-receive-props, render, and render-state

   Everything inside body is within an implicit reify; any other non-Om protocols
   can be specified if needed.

   Example:

   (defcomponent widget [data owner]
     (init-state [this] {:count 0})
     (render [this] (dom/h1 nil (:text data)))

   (defn widget [data owner]
     (reify
       om/IInitState
       (init-state [this] {:count 0})
       om/IRender
       (render [this]
         (dom/h1 nil (:text data)))))"
  [name & args]
  (let [[doc-string? args] (maybe-split-first string? args)
        [attr-map? more-args] (maybe-split-first map? args)
        [arglist & forms] args]
    `(defn ~name
       ~@(when doc-string? [doc-string?])
       ~@(when attr-map? [attr-map?])
       ~arglist
       (reify
         ~@(add-component-protocols forms)))))

(defmacro component
  "Sugar for defining anonymous Om component. See defcomponent for details."
  [& forms]
  `(reify ~@(add-component-protocols forms)))
