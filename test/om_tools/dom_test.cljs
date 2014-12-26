(ns om-tools.dom-test
  (:require
   [cemerick.cljs.test :refer-macros [is deftest testing are]]
   [om.dom :as om-dom :include-macros true]
   [om-tools.dom :as dom :include-macros true]))

(def +react-dom-prototype+ (.-prototype (js/React.DOM.span nil)))

(defn react-dom? [x]
  (and x (= (.-prototype x) +react-dom-prototype+)))

(defn props [el]
  (js->clj (.-props el) :keywordize-keys true))

(defn children [el]
  (:children (props el)))

(defn is=el [el1 el2]
  (is (= (.-tagName el1) (.-tagName el2)))
  (let [el1-props (props el1)
        el2-props (props el2)
        el1-children (:children el1-props)
        el2-children (:children el2-props)]

    (is (= (dissoc el1-props :children)
           (dissoc el2-props :children)))

    (cond
     (every? coll? [el1-children el2-children])
     (doseq [[c1 c2] (map vector (:children el1-props) (:children el2-props))]
       (is=el c1 c2))

     (every? react-dom? [el1-children el2-children])
     (is=el el1-children el2-children)

     :else (is (= el1-children el2-children)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Tests

(deftest camel-case-test
  (are [expected s] (= expected (dom/camel-case s))
       "" ""
       "foo" "foo"
       "fooBar" "foo-bar"
       "fooBarBaz" "foo-bar-baz"
       "fooBarBaz" "fooBarBaz"))

(deftest opt-key-case-test
  (is (= "acceptCharset" (dom/opt-key-case "accept-charset")))
  (is (= "httpEquiv" (dom/opt-key-case "http-equiv")))

  (is (= "test" (dom/opt-key-case "test")))

  (testing "data-* attributes not camel-cased"
    (is (= "data-test"    (dom/opt-key-case "data-test")))
    (is (= "data-foo-bar" (dom/opt-key-case "data-foo-bar"))))

  (testing "aria-* attributes not camel-cased"
    (is (= "aria-test"    (dom/opt-key-case "aria-test")))
    (is (= "aria-foo-bar" (dom/opt-key-case "aria-foo-bar")))))

(deftest class-set-test
  (testing "nil when no truthy values"
    (is (nil? (dom/class-set {})))
    (is (nil? (dom/class-set {"foo" false})))
    (is (nil? (dom/class-set {"foo" nil}))))

  (testing "simple"
    (is (= "foo" (dom/class-set {"foo" true}))))

  (testing "class as keyword"
    (is (= "foo" (dom/class-set {:foo true}))))

  (testing "multiple classes"
    (is (= "foo bar" (dom/class-set {:foo true :bar true}))))

  (testing "duplicate classes"
    (is (= "foo" (dom/class-set {:foo true "foo" true})))))

(deftest element-test
  (testing "simple element"
    (is=el (dom/element js/React.DOM.a {:href "/"} ["foo" "bar"])
           (om-dom/a #js {:href "/"} "foo" "bar")))

  (testing "opt formatting"
    (is=el (dom/element js/React.DOM.a {:on-click println} "foo")
           (om-dom/a #js {:onClick println} "foo")))

  (testing "class -> className"
    (is=el (dom/element js/React.DOM.a {:class "bar"} "foo")
           (om-dom/a #js {:className "bar"} "foo")))

  (testing "for -> htmlFor"
    (is=el (dom/element js/React.DOM.label {:for "bar"} "foo")
           (om-dom/label #js {:htmlFor "bar"} "foo"))))

(deftest om-equiv-test
  (testing "simple tag"
    (is=el (dom/a "test") (om-dom/a nil "test")))

  (testing "zero args"
    (is=el (dom/br) (om-dom/br nil)))

  (testing "simple opts"
    (is=el (dom/a {:href "/test"} "test")
           (om-dom/a #js {:href "/test"} "test")))

  (testing "map opt value"
    (is=el (dom/div {:style {:color "blue"}})
           (om-dom/div #js {:style #js {:color "blue"}}))
    (is=el (dom/div {:dangerouslySetInnerHTML {:__html "<p>foo</p>"}})
           (om-dom/div #js {:dangerouslySetInnerHTML #js {:__html "<p>foo</p>"}})))

  (testing "runtime opts"
    (is=el (dom/a (when true {:href "/test"}) "test")
           (om-dom/a (when true #js {:href "/test"}) "test"))
    (doseq [non-opt [(dom/div "ok?") "ok?" :ok? 0 0.0 true false js/undefined]]
      (is=el (dom/div non-opt)
             (om-dom/div nil non-opt))))

  (testing "runtime children"
    (is=el (dom/a (when true "foo") "bar")
           (om-dom/a nil (when true "foo") "bar"))
    (is=el (dom/a (when true "foo"))
           (om-dom/a nil (when true "foo")))
    (is=el (dom/a (when true "foo") ["bar" "baz"])
           (apply om-dom/a nil (when true "foo") ["bar" "baz"])))

  (testing "runtime flatten"
    (is=el (dom/a (when true {:href "/test"}) ["foo" ["bar"]])
           (om-dom/a (when true #js {:href "/test"}) "foo" "bar")))

  (testing "macro flatten"
    (is=el (dom/a ["foo" ["bar"]])
           (om-dom/a nil "foo" "bar")))

  (testing "om non-macro tags"
    (is=el (dom/input {:value "test"})
           (om-dom/input #js {:value "test"})))

  (testing "simple nesting"
    (is=el (dom/div (dom/span "test"))
           (om-dom/div nil (om-dom/span nil "test"))))

  (testing "nesting with opts"
    (is=el (dom/div {:id "test"} (dom/span "test"))
           (om-dom/div #js {:id "test"} (om-dom/span nil "test"))))

  (testing "nesting without opts"
    (is=el (dom/div (dom/span "test")
                    (for [x ["foo" "bar"]] (dom/em x)))
           (apply om-dom/div nil
                  (om-dom/span nil "test")
                  (for [x ["foo" "bar"]]
                    (om-dom/em nil x)))))

  (testing "nesting with opts"
    (is=el (children (dom/div {:id "test"} (dom/span "test")))
           (children (om-dom/div #js {:id "test"} (om-dom/span nil "test")))))

  (testing "seq children"
    (let [xs (range 10)
          el (dom/ul (for [x xs] (dom/li x)))
          om-el (apply om-dom/ul nil (for [x xs] (om-dom/li nil x)))
          c (children el)
          om-c (children om-el)]
      (is (= (.-tagName el) (.-tagName om-el)))
      (doseq [i xs]
        (is=el (nth c i) (nth om-c i)))))

  (testing "var in opts"
    (is=el (om-dom/div #js {:style #js {:backgroundColor "red"}})
           (let [style {:background-color "red"}]
             (dom/div {:style style})))
    (is=el (om-dom/div #js {:style #js {:backgroundColor "red"}})
           (dom/div {:style (merge {:background-color "red"})}))
    (is=el (om-dom/div #js {:style #js {:backgroundColor "red"}})
           (dom/div {:style (when true {:background-color "red"})})))

  (testing "taking refernce of dom fns at runtime"
    (let [h1 dom/h1
          om-h1 om-dom/h1]
      (is=el (h1 {} "Heading")
             (om-h1 #js {} "Heading")))
    (let [input dom/input
          om-input om-dom/input]
      (is=el (input {:type "text"})
             (om-input #js {:type "text"}))))

  (testing "js values still work"
    (is=el (dom/div #js {} (dom/span #js {}))
           (om-dom/div #js {} (om-dom/span #js {})))
    (is=el (dom/div #js {:className "foo"} "bar")
           (om-dom/div #js {:className "foo"} "bar"))
    (is=el (dom/div #js {:className "foo" :class {:display "block"}})
           (om-dom/div #js {:className "foo" :class {:display "block"}}))))
