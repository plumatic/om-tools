(ns examples.mixin
  (:require
    [om.core :as om]
    [om-tools.core :refer-macros [defcomponentk]]
    [om-tools.dom :as dom :include-macros true]
    [om-tools.mixin :refer-macros [defmixin]]))

(defmixin set-interval-mixin
  (will-mount [owner]
    (set! (. owner -intervals) #js []))
  (will-unmount [owner]
    (.. owner -intervals (map js/clearInterval)))
  (set-interval [owner f t]
    (.. owner -intervals (push (js/setInterval f t)))))

(defcomponentk tick-tock [owner state]
  (:mixins set-interval-mixin)
  (init-state [_]
    {:seconds 0})
  (did-mount [_]
    (.set-interval owner #(swap! state update-in [:seconds] inc) 1000))
  (render [_]
    (dom/p
      (str "React has been running for " (:seconds @state) " seconds."))))

(defcomponentk example []
  (render [_]
    (->tick-tock {})))

(om/root
  example {}
  {:target (. js/document (getElementById "example"))})
