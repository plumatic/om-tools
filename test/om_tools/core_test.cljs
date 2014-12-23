(ns om-tools.core-test
  (:require-macros
   [om-tools.test-utils :refer [with-element]])
  (:require
   [cemerick.cljs.test :refer-macros [is are deftest testing use-fixtures done]]
   [clojure.set :as set]
   [om-tools.core :as om-tools :refer-macros [defcomponent defcomponentk defcomponentmethod]]
   [om-tools.dom :as dom :include-macros true]
   [om.core :as om]
   [schema.core :as s :include-macros true]
   [schema.test :as schema-test]))

(enable-console-print!)

(def ReactTestUtils (.. js/React -addons -TestUtils))

(s/defschema TestComponent
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

(defcomponent test-default-display-name-component
  [data owner]
  (render [_] :render))

(defcomponent ^:always-validate test-always-validate-component
  [data :- {:items [s/Keyword]} owner]
  (render [_] (apply str (:items data))))

(defcomponent ^:never-validate test-never-validate-component
  [data :- {:items [s/Keyword]} owner]
  (render [_] (apply str (:items data))))

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
    (is (. js/React (isValidComponent (->test-component {:foo "foo" :bar "bar"})))))
  (testing "default display-name"
    (let [c (test-default-display-name-component {} nil)]
      (is (satisfies? om/IDisplayName c))
      (is (= "test-default-display-name-component" (om/display-name c)))))
  (testing "schema metadata"
    (s/with-fn-validation
      (is (test-never-validate-component {:items :not-expected} nil)
          "Component marked ^:never-validate should not throw (when validation turned on)"))
    (s/without-fn-validation
     (is (thrown? js/Error (test-always-validate-component {:items :not-expected} nil))
         "Component marked ^:always-validate should throw (when validation turned off)"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defcomponentk test-default-display-name-componentk
  []
  (render [_] :render))

(defcomponentk ^:always-validate test-always-validate-componentk
  [[:data items :- [s/Keyword]]]
  (render [_] (apply str items)))

(defcomponentk ^:never-validate test-never-validate-componentk
  [[:data items :- [s/Keyword]]]
  (render [_] (apply str items)))

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
    (s/with-fn-validation
      (is (thrown? js/Error (test-componentk {:foo :bar :bar "bar"} nil)))
      (is (thrown? js/Error (test-componentk {:foo {} :bar "bar"} nil)))))
  (testing "build constructor"
    (is (. js/React (isValidComponent (->test-componentk {:foo "foo" :bar "bar"})))))
  (testing "default display-name"
    (let [c (test-default-display-name-componentk {} nil)]
      (is (satisfies? om/IDisplayName c))
      (is (= "test-default-display-name-componentk" (om/display-name c)))))
  (testing "schema metadata"
    (s/with-fn-validation
      (is (test-never-validate-componentk {:items :not-expected} nil)
          "Component marked ^:never-validate should not throw (when validation turned on)"))
    (s/without-fn-validation
     (is (thrown? js/Error (test-always-validate-componentk {:items :not-expected} nil))
         "Component marked ^:always-validate should throw (when validation turned off)"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcomponentk shared-data-component
  [[:shared api-host api-version]]
  (render [_]
    (dom/div (str api-host "/" api-version))))

(deftest defcomponentk-shared-test
  (with-element [e "div"]
    (om/root shared-data-component {}
             {:target e
              :shared {:api-host "api.example.com"
                       :api-version "1.5"}})
    (is (= "api.example.com/1.5" (.-innerText e)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
       (done)
       (.. js/document -body (removeChild e)))
     60)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti fruit-basket-item
  (fn [fruit owner] (:type fruit)))

(defcomponentmethod fruit-basket-item :default
  [fruit owner]
  (render [_]
    (dom/label (str "Unknown fruit: " (name (:type fruit))))))

(defcomponentmethod fruit-basket-item :orange
  [orange _]
  (render [_]
    (dom/label "Orange")))

(defcomponentmethod fruit-basket-item :banana
  [banana :- {:peeled? s/Bool, s/Any s/Any} _]
  (render [_]
    (dom/label (str "Banana" (when (:peeled? banana) " (peeled)")))))

(defcomponentmethod fruit-basket-item :apple
  [apple :- {:variety s/Keyword, s/Any s/Any} _]
  (render [_]
    (dom/label
     (if (= (:variety apple) :red-delicious)
       "Apple (gross)"
       "Apple"))))

(defcomponentk fruit-basket
  [[:data fruits :- [{:type s/Keyword, s/Any s/Any}]]]
  (render [_]
    (dom/div
     (om/build-all fruit-basket-item fruits))))

(deftest multicomponent-test
  (let [fruits [{:type :apple :variety :granny-smith}
                {:type :banana :peeled? true}
                {:type :apple :variety :red-delicious}
                {:type :pineapple}
                {:type :orange}]
        c (om/build fruit-basket {:fruits fruits})
        div (.createElement js/document "div")
        d (.renderComponent js/React c div)]
    (is (= ["Apple"
            "Banana (peeled)"
            "Apple (gross)"
            "Unknown fruit: pineapple"
            "Orange"]
           (for [label (.scryRenderedDOMComponentsWithTag ReactTestUtils d "label")]
             (.. label -props -children))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest set-state?!-test
  (let [mem (atom {})
        calls (atom {})
        owner (specify! (clj->js om/pure-methods)
                om/IGetState
                (-get-state [this]
                  @mem)
                (-get-state [this ks]
                  (get-in @mem ks))
                om/ISetState
                (-set-state! [this val render]
                  (swap! calls update-in [::root] (fnil inc 0))
                  (reset! mem val))
                (-set-state! [this ks val render]
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(use-fixtures :once schema-test/validate-schemas)
