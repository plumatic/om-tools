(ns om-tools.core-test
  (:require-macros
   [cemerick.cljs.test :refer [is deftest testing use-fixtures]])
  (:require
   cemerick.cljs.test
   [om-tools.core :as om-tools :include-macros true]
   [om.core :as om]
   [schema.core :as s]
   [schema.test :as schema-test]))

(defn composite-component?
  "http://git.io/BPd6uw"
  [x]
  (and (fn? (aget x "render"))
       (fn? (aget x "setState"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(om-tools/defcomponent core-test-component
  [[:data foo bar] :- {:foo s/Str :bar s/Str} owner]
  (display-name [_] (str foo bar "!"))
  (init-state [_] :init-state)
  (should-update [_ _ _] :should-update)
  (will-mount [_] :will-mount)
  (did-mount [_] :did-mount)
  (will-unmount [_] :will-unmount)
  (will-update [_ _ _] :will-update)
  (will-receive-props [_ _] :will-receive-props)
  (render [_] :render)
  (render-state [_ _] :render-state))

(deftest defcomponent-test
  (testing "defs"
    (is (fn? core-test-component))
    (is (fn? ->core-test-component)))
  (testing "construct object w/ lifecycle protocols"
    (let [c (core-test-component {:foo "foo" :bar "bar"} :owner)]
      (is (= "foobar!" (om/display-name c)))
      (is (= :init-state (om/init-state c)))
      (is (= :should-update (om/should-update c nil nil)))
      (is (= :will-mount (om/will-mount c)))
      (is (= :did-mount (om/did-mount c)))
      (is (= :will-unmount (om/will-unmount c)))
      (is (= :will-update (om/will-update c nil nil)))
      (is (= :will-receive-props (om/will-receive-props c nil)))
      (is (= :render (om/render c)))
      (is (= :render-state (om/render-state c nil)))))
  (testing "schema error"
    (is (thrown? js/Error (core-test-component {:foo :bar :bar "bar"} :owner)))
    (is (thrown? js/Error (core-test-component {:foo {} :bar "bar"} :owner))))
  (testing "build constructor"
    (is (composite-component? (->core-test-component {:foo "foo" :bar "bar"})))))

(use-fixtures :once schema-test/validate-schemas)
