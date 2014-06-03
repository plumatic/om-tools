(ns om-tools.core-test
  (:require
   [clojure.test :refer :all]
   [om-tools.core :as om-tools]))

(deftest test-add-component-protocols
  (is (= '(om.core/IDisplayName
           (display-name [this] ...)
           om.core/IInitState
           (init-state [this] ...)
           om.core/IShouldUpdate
           (should-update [this next-props next-state] ...)
           om.core/IWillMount
           (will-mount [this] ...)
           om.core/IDidMount
           (did-mount [this] ...)
           om.core/IWillUnmount
           (will-unmount [this] ...)
           om.core/IWillUpdate
           (will-update [this next-props next-state] ...)
           om.core/IDidUpdate
           (did-update [this prev-props prev-state] ...)
           om.core/IWillReceiveProps
           (will-receive-props [this next-props] ...)
           om.core/IRender
           (render [this] ...)
           om.core/IRenderState
           (render-state [this state] ...))
         (om-tools/add-component-protocols
          '((display-name [this] ...)
            (init-state [this] ...)
            (should-update [this next-props next-state] ...)
            (will-mount [this] ...)
            (did-mount [this] ...)
            (will-unmount [this] ...)
            (will-update [this next-props next-state] ...)
            (did-update [this prev-props prev-state] ...)
            (will-receive-props [this next-props] ...)
            (render [this] ...)
            (render-state [this state] ...))))))
#_
(deftest test-def-component
  (is (= (macroexpand
          '(clojure.core/defn widget
             [data owner]
             (clojure.core/reify
               om.core/IInitState
               (init-state [this] {:count 0})
               om.core/IRender
               (render [this] (om.dom/h1 nil (:text data))))))
         (macroexpand
          '(om-tools.core/defcomponent widget
             [data owner]
             (init-state [this] {:count 0})
             (render [this] (om.dom/h1 nil (:text data)))))))

  (testing "docstring"
    (is (= (macroexpand
            '(clojure.core/defn widget
               "docstring"
               [data owner]
               (clojure.core/reify
                 om.core/IRender
                 (render [this] (om.dom/h1 nil (:text data))))))
           (macroexpand
            '(om-tools.core/defcomponent widget
               "docstring"
               [data owner]
               (render [this] (om.dom/h1 nil (:text data)))))))))

(deftest test-component
  (is (= (macroexpand
          '(clojure.core/reify
             om.core/IInitState
             (init-state [this] {:count 0})
             om.core/IRender
             (render [this] (om.dom/h1 nil (:text data)))))
         (macroexpand
          '(om-tools.core/component
            (init-state [this] {:count 0})
            (render [this] (om.dom/h1 nil (:text data))))))))
