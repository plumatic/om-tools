(ns om-tools.dom-test
  (:require-macros
   [cemerick.cljs.test :refer [is deftest testing are]])
  (:require
   cemerick.cljs.test
   [om.dom :as om-dom :include-macros true]
   [om-tools.dom :as dom :include-macros true]))

(defn props [el]
  (js->clj (.-props el) :keywordize-keys true))

(defn children [el]
  (:children (props el)))

(defn is=el [el1 el2]
  (is (= (.-tagName el1) (.-tagName el2)))
  (is (= (props el1) (props el2))))

(deftest el
  (is=el (dom/el js/React.DOM.a {:href "/"} ["foo" "bar"])
         (om-dom/a #js {:href "/"} "foo" "bar")))

(deftest om-equiv
  (testing "simple tag"
    (is=el (dom/a "test") (om-dom/a nil "test")))

  (testing "simple opts"
    (is=el (dom/a {:href "/test"} "test")
           (om-dom/a #js {:href "/test"} "test")))

  (testing "runtime template"
    (is=el (dom/a (when true {:href "/test"}) "test")
           (om-dom/a (when true #js {:href "/test"}) "test")))

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
    (is=el (children (dom/div (dom/span "test")))
           (children (om-dom/div nil (om-dom/span nil "test")))))

  (testing "seq children"
    (let [xs (range 10)
          el (dom/ul (for [x xs] (dom/li x)))
          om-el (apply om-dom/ul nil (for [x xs] (om-dom/li nil x)))
          c (children el)
          om-c (children om-el)]
      (is (= (.-tagName el) (.-tagName om-el)))
      (doseq [i xs]
        (is=el (nth c i) (nth om-c i))))))
