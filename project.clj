(defproject prismatic/om-tools "0.3.6"
  :description "Tools for building Om applications"
  :url "http://github.com/prismatic/om-tools"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}

  :dependencies [[prismatic/plumbing "0.3.2"]
                 [prismatic/schema "0.2.4"]]

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.6.0"]
                                  [org.clojure/clojurescript "0.0-2202"]
                                  [om "0.7.1"]
                                  [com.keminglabs/cljx "0.3.1"]
                                  [prismatic/dommy "0.1.2"]]
                   :plugins [[com.keminglabs/cljx "0.3.1"]
                             [lein-cljsbuild "1.0.3"]
                             [com.cemerick/clojurescript.test "0.3.0"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl
                                                     cljx.repl-middleware/wrap-cljx]}
                   :cljx
                   {:builds [{:source-paths ["src"]
                              :output-path "target/generated/src"
                              :rules :clj}
                             {:source-paths ["src"]
                              :output-path "target/generated/src"
                              :rules :cljs}]}}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}}

  :aliases {"all" ["with-profile" "dev:dev,1.5"]
            "deploy" ["do" "clean," "cljx" "once," "deploy" "clojars"]
            "test" ["do" "clean," "cljx" "once," "test," "with-profile" "dev" "cljsbuild" "test"]}

  :jar-exclusions [#"\.cljx|\.swp|\.swo|\.DS_Store"]

  :lein-release {:deploy-via :shell
                 :shell ["lein" "deploy"]}

  :source-paths ["target/generated/src" "src"]

  :cljsbuild
  {:test-commands {"unit" ["phantomjs" :runner
                           "test/vendor/es5-shim.js"
                           "test/vendor/es5-sham.js"
                           "test/vendor/console-polyfill.js"
                           "target/om_tools.js"]}
   :builds [{:id "test"
             :source-paths ["src" "test" "target/generated/src"]
             :compiler {:output-to "target/om_tools.js"
                        :optimizations :whitespace
                        :pretty-print true
                        :preamble ["react/react_with_addons.js"]
                        :externs ["react/externs/react.js"]}}
            {:id "example/sliders"
             :source-paths ["src" "target/generated/src" "examples/sliders/src"]
             :compiler {:output-to "examples/sliders/main.js"
                        :output-dir "examples/sliders/out"
                        :optimizations :none}}
            {:id "example/mixin"
             :source-paths ["src" "target/generated/src" "examples/mixin/src"]
             :compiler {:output-to "examples/mixin/main.js"
                        :output-dir "examples/mixin/out"
                        :optimizations :none}}]})
