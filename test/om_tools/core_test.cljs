(ns om-tools.core-test
  (:require-macros
   [cemerick.cljs.test :refer [is are deftest testing use-fixtures done]]
   [schema.macros :as sm])
  (:require
   cemerick.cljs.test
   [om-tools.core :as om-tools :refer-macros [defcomponent defcomponentk]]
   [om-tools.dom :as dom :include-macros true]
   [om.core :as om]
   [schema.core :as s]
   [schema.test :as schema-test]))

(defn composite-component?
  "http://git.io/BPd6uw"
  [x]
  (and (fn? (aget x "render"))
       (fn? (aget x "setState"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(sm/defschema TestComponent
  {:foo s/Str :bar s/Str})

(defcomponent test-component
  [data :- TestComponent owner]
  (display-name [_] (str (:foo data) (:bar data) "!"))
  (init-state [_] :init-state)
  (should-update [_ _ _] :should-update)
  (will-mount [_] :will-mount)
  (did-mount [_] :did-mount)
  (will-unmount [_] :will-unmount)
  (will-update [_ _ _] :will-update)
  (did-update [_ _ _] :did-update)
  (will-receive-props [_ _] :will-receive-props)
  (render [_] :render)
  (render-state [_ _] :render-state))

(deftest defcomponent-test
  (testing "defs"
    (is (fn? test-component))
    (is (fn? ->test-component)))
  (testing "construct object w/ lifecycle protocols"
    (let [c (test-component {:foo "foo" :bar "bar"} nil)]
      (is (= "foobar!" (om/display-name c)))
      (is (= :init-state (om/init-state c)))
      (is (= :should-update (om/should-update c nil nil)))
      (is (= :will-mount (om/will-mount c)))
      (is (= :did-mount (om/did-mount c)))
      (is (= :will-unmount (om/will-unmount c)))
      (is (= :will-update (om/will-update c nil nil)))
      (is (= :did-update (om/did-update c nil nil)))
      (is (= :will-receive-props (om/will-receive-props c nil)))
      (is (= :render (om/render c)))
      (is (= :render-state (om/render-state c nil)))))
  (testing "schema error"
    (is (thrown? js/Error (test-component {:foo :bar :bar "bar"} nil)))
    (is (thrown? js/Error (test-component {:foo {} :bar "bar"} nil))))
  (testing "build constructor"
    (is (composite-component? (->test-component {:foo "foo" :bar "bar"})))))

(defcomponentk test-componentk
  [[:data foo bar] :- TestComponent owner]
  (display-name [_] (str foo bar "!"))
  (init-state [_] :init-state)
  (should-update [_ _ _] :should-update)
  (will-mount [_] :will-mount)
  (did-mount [_] :did-mount)
  (will-unmount [_] :will-unmount)
  (will-update [_ _ _] :will-update)
  (did-update [_ _ _] :did-update)
  (will-receive-props [_ _] :will-receive-props)
  (render [_] :render)
  (render-state [_ _] :render-state))

(deftest defcomponentk-test
  (testing "defs"
    (is (fn? test-componentk))
    (is (fn? ->test-componentk)))
  (testing "construct object w/ lifecycle protocols"
    (let [c (test-componentk {:foo "foo" :bar "bar"} nil)]
      (is (= "foobar!" (om/display-name c)))
      (is (= :init-state (om/init-state c)))
      (is (= :should-update (om/should-update c nil nil)))
      (is (= :will-mount (om/will-mount c)))
      (is (= :did-mount (om/did-mount c)))
      (is (= :will-unmount (om/will-unmount c)))
      (is (= :will-update (om/will-update c nil nil)))
      (is (= :did-update (om/did-update c nil nil)))
      (is (= :will-receive-props (om/will-receive-props c nil)))
      (is (= :render (om/render c)))
      (is (= :render-state (om/render-state c nil)))))
  (testing "schema error"
    (is (thrown? js/Error (test-componentk {:foo :bar :bar "bar"} nil)))
    (is (thrown? js/Error (test-componentk {:foo {} :bar "bar"} nil))))
  (testing "build constructor"
    (is (composite-component? (->test-componentk {:foo "foo" :bar "bar"})))))

(defcomponentk shared-data-component
  [[:shared api-host api-version]]
  (render [_]
    (dom/div (str api-host "/" api-version))))

(deftest defcomponentk-shared-test
  (let [e (.createElement js/document "div")]
    (.. js/document -body (appendChild e))
    (om/root shared-data-component {}
             {:target e
              :shared {:api-host "api.example.com"
                       :api-version "1.5"}})
    (is (= "api.example.com/1.5" (.-innerText e)))))

(defcomponentk stateful-component [data state]
  (did-mount [_]
    (js/setTimeout #(swap! state assoc :y 2) 20))
  (render [_]
    (dom/div
     (let [{:keys [x y]} @state]
       (str "x=" x ",y=" (or y "nil"))))))

(deftest ^:async state-proxy-test
  (let [e (.createElement js/document "div")]
    (.. js/document -body (appendChild e))
    (om/root stateful-component {} {:target e :init-state {:x 5}})
    (is (= "x=5,y=nil" (.-innerText e)))
    (om/root stateful-component {} {:target e :state {:x 6}})
    (is (= "x=6,y=nil" (.-innerText e)))
    (js/setTimeout
     (fn []
       (testing "swapped on state"
         (is (= "x=6,y=2" (.-innerText e))))
       (done))
     60)))

(deftest set-state?!-test
  (let [mem (atom {})
        calls (atom {})
        owner (reify
                om/IGetState
                (-get-state [this]
                  @mem)
                (-get-state [this ks]
                  (get-in @mem ks))
                om/ISetState
                (-set-state! [this val]
                  (swap! calls update-in [::root] (fnil inc 0))
                  (reset! mem val))
                (-set-state! [this ks val]
                  (swap! calls update-in ks (fnil inc 0))
                  (swap! mem assoc-in ks val)))]
    (is (not (nil? (om-tools/set-state?! owner {:bar "bar"}))))
    (is (= 1 (::root @calls)))
    (is (nil? (om-tools/set-state?! owner {:bar "bar"})))
    (is (= 1 (::root @calls)))
    (is (not (nil? (om-tools/set-state?! owner :foo "foo"))))
    (is (= 1 (:foo @calls)))
    (is (nil? (om-tools/set-state?! owner :foo "foo")))
    (is (= 1 (:foo @calls)))
    (is (not (nil? (om-tools/set-state?! owner :foo "foo2"))))
    (is (= 2 (:foo @calls)))
    (is (not (nil? (om-tools/set-state?! owner [:baz :qux] 42))))
    (is (= 1 (get-in @calls [:baz :qux])))
    (is (nil? (om-tools/set-state?! owner [:baz :qux] 42)))
    (is (= 1 (get-in @calls [:baz :qux])))))

(defcomponent component-with-docstring
  "component docstring"
  [data owner]
  (render [_] (dom/div nil "")))

(defcomponent component-with-attr-map
  {:attr true}
  [data owner]
  (render [_] (dom/div nil "")))

(defcomponent component-with-docstring-and-attr-map
  "component docstring"
  {:attr true}
  [data owner]
  (render [_] (dom/div nil "")))

(defcomponent component-with-prepost-map
  [data owner]
  {:pre (constantly true) :post (constantly true)}
  (render [_] (dom/div nil "")))

(defcomponentk componentk-with-docstring
  "component docstring"
  [data owner]
  (render [_] (dom/div nil "")))

(defcomponentk componentk-with-attr-map
  {:attr true}
  [data owner]
  (render [_] (dom/div nil "")))

(defcomponentk componentk-with-docstring-and-attr-map
  "component docstring"
  {:attr true}
  [data owner]
  (render [_] (dom/div nil "")))

(defcomponentk componentk-with-prepost-map
  [data owner]
  {:pre (constantly true) :post (constantly true)}
  (render [_] (dom/div nil "")))

(deftest defcomponent-args-test
  (are [component] (fn? component)
       component-with-docstring
       component-with-attr-map
       component-with-docstring-and-attr-map
       component-with-prepost-map
       componentk-with-docstring
       componentk-with-attr-map
       componentk-with-docstring-and-attr-map
       componentk-with-prepost-map))

(use-fixtures :once schema-test/validate-schemas)
