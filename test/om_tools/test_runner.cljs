(ns om-tools.test-runner
  (:require  [cemerick.cljs.test :as t :include-macros true]
             [om-tools.core-test]
             [om-tools.dom-test]
             [om-tools.mixin-test]
             [om-tools.schema-test]))

(enable-console-print!)

(t/run-tests 'om-tools.core-test
             'om-tools.dom-test
             'om-tools.mixin-test
             'om-tools.schema-test)
