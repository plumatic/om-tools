(ns om-tools.schema-test
  (:require-macros
   [schema.macros :as sm])
  (:require
   [om-tools.schema :as schema]
   [om-tools.core :as ot :include-macros true :refer [defcomponent]]
   [om-tools.dom :as dom :include-macros true]
   [schema.core :as s]
   [schema.test :as schema-test]
   [om.core :as om]
   [cemerick.cljs.test :as t :include-macros true :refer [deftest is use-fixtures]]))

(sm/defschema Item
  {:id s/Int :name s/Str})

(sm/defschema App
  {:items [Item]})

(sm/defschema ItemList
  {:items (schema/cursor [Item])
   :order s/Keyword})

(defcomponent item-list [[:data items order] :- ItemList]
  (render [_]
    (let [comp-fn (if (= order :asc) compare (comp - compare))]
      (dom/ul
       (for [u (sort-by :name comp-fn items)]
         (dom/li (:name u)))))))

(defcomponent app [[:data items] :- App]
  (render [_]
    (dom/div
     (om/build item-list
               {:items items
                :order :asc}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Test utils

(def ^:dynamic *container* nil)

(defn container-fixture [test]
  (let [parent (.-body js/document)
        e (.createElement js/document "div")]
    (.appendChild parent e)
    (set! *container* e)
    (test)
    (.removeChild parent e)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Tests

(deftest app-schema-test
  (om/root
   app
   {:items [{:id 1 :name "Common Lisp"}
            {:id 2 :name "Clojure"}
            {:id 3 :name "ClojureScript"}]}
   {:target *container*})
  (is (not (clojure.string/blank? (.-innerHTML *container*)))))

(use-fixtures :once
  (t/compose-fixtures
   schema-test/validate-schemas
   container-fixture))
