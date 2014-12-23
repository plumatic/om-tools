(ns om-tools.mixin-test
  (:require-macros
   [om-tools.test-utils :refer [with-element]])
  (:require
   [cemerick.cljs.test :refer-macros [is are deftest testing use-fixtures done]]
   [om-tools.mixin :refer-macros [defmixin]]
   [om-tools.core :as om-tools :refer-macros [defcomponent defcomponentk]]
   [om-tools.dom :as dom :include-macros true]
   [om.core :as om]))

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

(def test-mixin-events (atom []))

(defmixin test-mixin
  (will-mount [owner]
    (om/set-state! owner :mixin-mounted? true))
  (will-update [owner next-props next-state]
    (swap! test-mixin-events conj [:will-update @next-props #_next-state]))
  (did-update [owner prev-props prev-state]
    ;; TODO Fix testing prev-state.
    ;; There appears to be new behavior in om 0.8.0-beta1+ where
    ;; component's hooks are called after mixin's
    (swap! test-mixin-events conj [:did-update @prev-props #_prev-state])))


(defcomponent component-with-mixin [data owner]
  (:mixins test-mixin)
  (render-state [_ {:keys [mixin-mounted?]}]
    (dom/div nil (if mixin-mounted?
                   "mixin-mounted"))))

(defcomponent wrapper-component-with-mixin [data owner]
  (render [_]
    (->component-with-mixin data)))

(deftest defcomponent-defmixin-test
  (is (object? component-with-mixin$descriptor))
  (with-element [e "div"]
    (om/root component-with-mixin {:version 0}
             {:target e
              :descriptor component-with-mixin$descriptor})
    (is (= [[:will-update {:version 0} #_{:mixin-mounted? true}]
            [:did-update  {:version 0} #_{}]]
           @test-mixin-events))
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
    (is (= [[:will-update {:version 0} #_{:mixin-mounted? true}]
            ;; TODO Fix testing prev-state
            ;; <  om 0.8.0-beta1: prev-state={:mixin-mounted? true}
            ;; >= om 0.8.0-beta1: prev-state={}
            [:did-update  {:version 0} #_{}]
            [:will-update {:version 1} #_{:mixin-mounted? true}]
            [:did-update  {:version 1} #_{}]]
           @test-mixin-events))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmixin set-interval-mixin
  (will-mount [owner]
    (set! (. owner -intervals) #js []))
  (will-unmount [owner]
    (.. owner -intervals (map js/clearInterval)))
  (set-interval [owner f t]
    (.. owner -intervals (push (js/setInterval f t)))))

(defcomponent tick-tock [data owner]
  (:mixins set-interval-mixin)
  (did-mount [_]
    (.set-interval owner #(om/transact! data :seconds (fn [s] (+ 0.01 s))) 10))
  (render-state [_ {:keys [seconds] :as m}]
    (dom/p {} "")))

(deftest ^:async tick-tock-test
  (let [e (.createElement js/document "div")
        data (atom {:seconds 0.0})]
    (.. js/document -body (appendChild e))
    (om/root tick-tock
             data
             {:target e
              :descriptor tick-tock$descriptor})
    (testing "interval cleared when unmounted"
      (js/setTimeout
       (fn []
         (. js/React (unmountComponentAtNode e))
         (let [seconds (:seconds @data)]
           (is (pos? seconds))
           (js/setTimeout
            (fn []
              (is (= (:seconds @data) seconds))
              (done)
              (.. js/document -body (removeChild e)))
            12)))
       92))))
