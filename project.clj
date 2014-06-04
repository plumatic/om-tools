(defproject om-tools "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://github.com/prismatic/om-tools"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2202"]
                 [com.keminglabs/cljx "0.3.1"]
                 [om "0.6.4"]
                 [prismatic/plumbing "0.3.1"]
                 [prismatic/schema "0.2.3"]]

  :plugins [[com.keminglabs/cljx "0.3.1"]
            [lein-cljsbuild "1.0.3"]
            [com.cemerick/clojurescript.test "0.3.0"]]

  :cljx
  {:builds [{:source-paths ["src"]
             :output-path "target/generated/src"
             :rules :clj}
            {:source-paths ["src"]
             :output-path "target/generated/src"
             :rules :cljs}]}

  :source-paths ["src" "target/generated/src"]

  :hooks [leiningen.cljsbuild]

  :cljsbuild
  {:test-commands {"unit" ["phantomjs" :runner
                           "test/vendor/es5-shim.js"
                           "test/vendor/es5-sham.js"
                           "test/vendor/console-polyfill.js"
                           "this.literal_js_was_evaluated=true"
                           "target/om_tools.js"]}
   :builds [{:id "om-tools"
             :source-paths ["src" "test" "target/generated/src"]
             :compiler {:output-to "target/om_tools.js"
                        :optimizations :whitespace
                        :pretty-print true
                        :preamble ["react/react.min.js"]
                        :externs ["react/externs/react.js"]}}]})
