(ns om-tools.core-test
  (:require
   [clojure.test :refer :all]
   [om-tools.core :as om-tools]
   [om.core :as om]))

(deftest test-partial-spec->spec-map
  (is (= {`om/IInitState ['(init-state [_] ...)]
          `om/IRenderState ['(render-state [_ state] ...)]
          'SomeOtherProtocol ['(some-method [_] ...)
                              '(some-other-method [_] ...)]}
         (om-tools/partial-spec->spec-map
          '((init-state [_] ...)
            (render-state [_ state] ...)
            SomeOtherProtocol
            (some-method [_] ...)
            (some-other-method [_] ...))))))

(deftest test-component-spec
  (are [full-spec]
    (let [partial-spec (rest full-spec)]
      (= full-spec (om-tools/component-spec partial-spec)))

    '(om.core/IDisplayName (display-name [this] ...))
    '(om.core/IInitState (init-state [this] ...))
    '(om.core/IShouldUpdate (should-update [this next-props next-state] ...))
    '(om.core/IWillMount (will-mount [this] ...))
    '(om.core/IDidMount (did-mount [this] ...))
    '(om.core/IWillUnmount (will-unmount [this] ...))
    '(om.core/IWillUpdate (will-update [this next-props next-state] ...))
    '(om.core/IDidUpdate (did-update [this prev-props prev-state] ...))
    '(om.core/IWillReceiveProps (will-receive-props [this next-props] ...))
    '(om.core/IRender (render [this] ...))
    '(om.core/IRenderState (render-state [this state] ...)))

  (testing "default spec-map"
    (is (= '(om.core/IDisplayName (display-name [this] "DisplayName"))
           (om-tools/component-spec
            []
            {`om/IDisplayName ['(display-name [this] "DisplayName")]})))))

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
            (render [this] (om.dom/h1 nil (:text data)))))))
  (is (thrown-with-msg? RuntimeException #"Unexpected form in body of component"
                        (macroexpand
                         '(om-tools.core/component
                           (render (om.dom/h1 nil (:text data))))))))

(deftest separate-component-config-test
  (are [forms out] (= out (om-tools/separate-component-config forms))
       '((:mixins a b c))
       [{:mixins '(a b c)} '()]

       '((render [_] ...) (will-mount [_] ...))
       [{} '((render [_] ...) (will-mount [_] ...))]

       '((render [_] ...) (:mixins [a]))
       [{:mixins '([a])} '((render [_] ...))]

       '((:opt1 true) (:opt2 false))
       [{:opt1 '(true) :opt2 '(false)} '()]

       '((:flag) (did-mount [_] ...))
       [{:flag '()} '((did-mount [_] ...))]))
