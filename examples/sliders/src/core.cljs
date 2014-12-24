(ns examples.sliders
  (:require
   [om.core :as om]
   [om-tools.core :refer-macros [defcomponentk]]
   [om-tools.dom :as dom :include-macros true]
   [plumbing.core :as p :include-macros true]))

(defcomponentk slider [[:data value {min 0} {max 100} :as cursor] state]
  (did-mount [_]
    (swap! state assoc :init-value value))
  (render [_]
    (dom/div
     (dom/input
      {:type "range"
       :min min
       :max max
       :value value
       :on-change #(om/update! cursor :value (js/parseInt (.. % -target -value) 10))
       :step 1})
     (dom/span nil (let [diff (- value (:init-value @state))]
                     (if (pos? diff) (str "+" diff) diff)))
     (dom/input
      {:type "reset"
       :on-click #(om/update! cursor :value (:init-value @state))}))))

(defcomponentk app
  [[:data sliders]]
  (render [_]
    (dom/div
     (for [slider sliders]
       (->slider slider))
     (dom/pre nil
              (pr-str sliders)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(om/root app
         {:sliders (mapv (fn [i] {:value (rand-int 100)}) (range 9))}
         {:target (.getElementById js/document "app")})
