(ns om-tools.util-test
  (:require
   [om-tools.util :as util]
   [clojure.test :refer :all]))

(deftest possibly-destructured?-test
  (are [form] (util/possibly-destructured? :foo form)
       '[foo]
       '[foo bar]
       '[bar foo]
       '[[:foo bar]]
       '[[:foo [:bar baz]]]
       '[bar :as m])
  (are [form] (not (util/possibly-destructured? :foo form))
       '[]
       '[bar]
       '[[:bar foo]]
       '[[:bar :as foo]]))
