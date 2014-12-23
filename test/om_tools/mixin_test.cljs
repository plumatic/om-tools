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

(def test-mixin-events (atom nil))

(defmixin test-mixin
  (will-mount [owner]
    (om/set-state! owner :mixin-mounted? true))
  (will-update [owner next-props next-state]
    (swap! test-mixin-events conj [:will-update @next-props next-state]))
  (did-update [owner prev-props prev-state]
    (swap! test-mixin-events conj [:did-update @prev-props prev-state]))
  (will-receive-props [owner next-props]
    (swap! test-mixin-events conj [:will-receive-props @next-props])))

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
    (om/root component-with-mixin {}
             {:target e
              :descriptor component-with-mixin$descriptor})
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
