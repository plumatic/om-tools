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

(sm/defschema ItemScores
  {s/Int s/Num})

(sm/defschema App
  {:items [Item]
   :item-scores ItemScores})

(sm/defschema ItemList
  {:items (schema/cursor [Item])      ;; om.core/IndexedCursor
   :scores (schema/cursor ItemScores) ;; om.core/MapCursor
   :order s/Keyword})

(defcomponent item-list [[:data items scores order] :- ItemList]
  (render [_]
    (let [comp-fn (if (= order :asc) compare (comp - compare))]
      (dom/ul
       (for [item (sort-by :name comp-fn items)]
         (dom/li
          (str (:name item) "-" (get scores (:id item)))))))))

(defcomponent app [[:data items item-scores] :- (schema/cursor App)]
  (render [_]
    (dom/div
     (om/build item-list
               {:items items
                :scores item-scores
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
            {:id 3 :name "ClojureScript"}]
    :item-scores {1 1.0, 2 1.0, 3 1.0}}
   {:target *container*})
  (is (not (clojure.string/blank? (.-innerHTML *container*)))))

(use-fixtures :once
  (t/compose-fixtures
   schema-test/validate-schemas
   container-fixture))
