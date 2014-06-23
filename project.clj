(defproject prismatic/om-tools "0.2.2"
  :description "Tools for building Om applications"
  :url "http://github.com/prismatic/om-tools"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2202"]
                 [com.keminglabs/cljx "0.3.1"]
                 [om "0.6.4"]
                 [prismatic/plumbing "0.3.2"]
                 [prismatic/schema "0.2.4"]]

  :plugins [[com.keminglabs/cljx "0.3.1"]
            [lein-cljsbuild "1.0.3"]
            [com.cemerick/clojurescript.test "0.3.0"]]

  :profiles {:dev {:dependencies [[prismatic/dommy "0.1.2"]]
                   :cljsbuild
                   {:builds
                    [{:id "example/sliders"
                      :source-paths ["src" "target/generated/src" "examples/sliders/src"]
                      :compiler {:output-to "examples/sliders/main.js"
                                 :output-dir "examples/sliders/out"
                                 :optimizations :none}}
                     {:id "example/mixin"
                      :source-paths ["src" "target/generated/src" "examples/mixin/src"]
                      :compiler {:output-to "examples/mixin/main.js"
                                 :output-dir "examples/mixin/out"
                                 :optimizations :none}}]}}}

  :lein-release {:deploy-via :shell
                 :shell ["lein" "deploy" "clojars"]}
  :cljx
  {:builds [{:source-paths ["src"]
             :output-path "target/generated/src"
             :rules :clj}
            {:source-paths ["src"]
             :output-path "target/generated/src"
             :rules :cljs}]}

  :prep-tasks ["cljx" "javac" "compile"]

  :source-paths ["src" "target/generated/src"]

  :hooks [leiningen.cljsbuild]

  :cljsbuild
  {:test-commands {"unit" ["phantomjs" :runner
                           "test/vendor/es5-shim.js"
                           "test/vendor/es5-sham.js"
                           "test/vendor/console-polyfill.js"
                           "this.literal_js_was_evaluated=true"
                           "target/om_tools.js"]}
   :builds [{:id "test"
             :source-paths ["src" "test" "target/generated/src"]
             :compiler {:output-to "target/om_tools.js"
                        :optimizations :whitespace
                        :pretty-print true
                        :preamble ["react/react.min.js"]
                        :externs ["react/externs/react.js"]}}]})
