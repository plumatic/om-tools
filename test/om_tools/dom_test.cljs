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

(def +react-dom-prototype+ (.-prototype (js/React.DOM.span nil)))

(defn react-dom? [x]
  (and (not (nil? x))
       (= (.-prototype x) +react-dom-prototype+)))

(defn is=el [el1 el2]
  (is (= (.-tagName el1) (.-tagName el2)))
  (let [el1-props (props el1)
        el2-props (props el2)
        el1-children (:children el1-props)
        el2-children (:children el2-props)]

    (is (= (dissoc el1-props :children)
           (dissoc el1-props :children)))

    (cond
     (every? coll? [el1-children el2-children])
     (doseq [[c1 c2] (map vector (:children el1-props) (:children el2-props))]
       (is=el c1 c2))

     (every? react-dom? [el1-children el2-children])
     (is=el el1-children el2-children)

     :else (is (= el1-children el2-children)))))

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

  (testing "seq children"
    (let [xs (range 10)
          el (dom/ul (for [x xs] (dom/li x)))
          om-el (apply om-dom/ul nil (for [x xs] (om-dom/li nil x)))
          c (children el)
          om-c (children om-el)]
      (is (= (.-tagName el) (.-tagName om-el)))
      (doseq [i xs]
        (is=el (nth c i) (nth om-c i))))))
