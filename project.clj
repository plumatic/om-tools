(defproject om-tools "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://github.com/prismatic/om-tools"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2202"]
                 [prismatic/plumbing "0.2.2"]
                 [om "0.6.2"]]

  :plugins [[lein-cljsbuild "1.0.3"]
            [com.cemerick/clojurescript.test "0.2.2"]]

  :source-paths ["src"]

  :cljsbuild
  {:test-commands {"unit" ["phantomjs" :runner
                           "test/polyfill.js"
                           "this.literal_js_was_evaluated=true"
                           "target/om_tools.js"]}
   :builds [{:id "om-tools"
             :source-paths ["src" "test"]
             :compiler {:output-to "target/om_tools.js"
                        :optimizations :whitespace
                        :preamble ["react/react.min.js"]
                        :externs ["react/externs/react.js"]}}]})
