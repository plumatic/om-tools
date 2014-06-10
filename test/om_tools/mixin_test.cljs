(ns om-tools.mixin-test
  (:require-macros
   [cemerick.cljs.test :refer [is are deftest testing use-fixtures done]]
   [om-tools.test-utils :refer [with-element]]
   [schema.macros :as sm])
  (:require
   cemerick.cljs.test
   [om-tools.mixin :refer-macros [defmixin]]
   [om-tools.core :as om-tools :refer-macros [defcomponent defcomponentk]]
   [om-tools.dom :as dom :include-macros true]
   [om.core :as om]
   [schema.core :as s]
   [schema.test :as schema-test]))

(defmixin stub-mixin
  (display-name [_] :display-name)
  (init-state [_] :init-state)
  (will-mount [_] :will-mount)
  (did-mount [_] :did-mount)
  (will-unmount [_] :will-unmount)
  (will-update [_ x y] [:will-update x y])
  (did-update [_ x y] [:did-update x y])
  (will-receive-props [_ x] [:will-receive-props x]))

(deftest defmixin-test
  (is (object? stub-mixin))
  (let [ks #{"getDisplayName"
             "getInitialState"
             "componentWillMount"
             "componentDidMount"
             "componentWillUnmount"
             "componentWillUpdate"
             "componentDidUpdate"
             "componentWillReceiveProps"}]
    (doseq [k ks]
      (testing (str k " is function")
        (is (fn? (aget stub-mixin k)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def test-mixin-events (atom nil))

(defmixin test-mixin
  (will-mount [this]
    (om/set-state! this :mixin-mounted? true))
  (will-update [this next-props next-state]
    (swap! test-mixin-events conj [:will-update @next-props next-state]))
  (did-update [this prev-props prev-state]
    (swap! test-mixin-events conj [:did-update @prev-props prev-state]))
  (will-receive-props [this next-props]
    (swap! test-mixin-events conj [:will-receive-props @next-props])))

(defcomponent component-with-mixin [data owner]
  (:mixins [test-mixin])
  (render-state [_ {:keys [mixin-mounted?]}]
    (dom/div nil (if mixin-mounted?
                   "mixin-mounted"))))

(defcomponent wrapper-component-with-mixin [data owner]
  (render [_]
    (->component-with-mixin data)))

(deftest defcomponent-defmixin-test
  (is (fn? component-with-mixin$ctor))
  (with-element [e "div"]
    (om/root component-with-mixin {}
             {:target e
              :ctor component-with-mixin$ctor})
    (is (= "mixin-mounted" (.-innerText e))))
  (with-element [e "div"]
    (reset! test-mixin-events [])
    (om/root wrapper-component-with-mixin
             {:version 0}
             {:target e})
    (is (= "mixin-mounted" (.-innerText e)))
    (om/root wrapper-component-with-mixin
             {:version 1}
             {:target e})
    (is (= "mixin-mounted" (.-innerText e)))
    (is (= [[:will-update {:version 0} {:mixin-mounted? true}]
            [:did-update  {:version 0} {:mixin-mounted? true}]
            [:will-update {:version 1} {:mixin-mounted? true}]
            [:did-update  {:version 1} {:mixin-mounted? true}]]
           @test-mixin-events))))
