(defproject prismatic/om-tools "0.4.1-SNAPSHOT"
  :description "Tools for building Om applications"
  :url "http://github.com/plumatic/om-tools"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}

  :dependencies [[prismatic/plumbing "0.5.0"]
                 [prismatic/schema "1.0.1"]
                 [org.clojure/clojurescript "0.0-2665" :scope "provided"]
                 [om "0.7.3" :scope "provided"]]

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.6.0"]]
                   :plugins [[com.keminglabs/cljx "0.5.0"]
                             [lein-cljsbuild "1.0.3"]
                             [com.cemerick/clojurescript.test "0.3.3"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}
             :om-0.7 {:dependencies [[org.clojure/clojurescript "0.0-2322"]
                                     [om "0.7.3"]]}
             :om-0.8 {:dependencies [[org.clojure/clojurescript "0.0-2505"]
                                     [om "0.8.0-beta5"]]}}

  :aliases {"all" ["with-profile" "+om-0.7:+om-0.8"]
            "deploy" ["do" "clean," "cljx" "once," "deploy" "clojars"]
            "test" ["do" "clean," "cljx" "once," "test," "with-profile" "+dev" "cljsbuild" "test"]}

  :jar-exclusions [#"\.cljx|\.swp|\.swo|\.DS_Store"]

  :lein-release {:deploy-via :shell
                 :shell ["lein" "deploy"]}

  :source-paths ["target/generated/src" "src"]

  :prep-tasks [["cljx" "once"]]

  :cljx
  {:builds [{:source-paths ["src"]
             :output-path "target/generated/src"
             :rules :clj}
            {:source-paths ["src"]
             :output-path "target/generated/src"
             :rules :cljs}]}

  :cljsbuild
  {:test-commands {"unit" ["phantomjs" :runner
                           "test/vendor/es5-shim.js"
                           "test/vendor/es5-sham.js"
                           "test/vendor/console-polyfill.js"
                           "target/test.js"]}
   :builds [{:id "test"
             :source-paths ["src" "test" "target/generated/src"]
             :compiler {:output-to "target/test.js"
                        :optimizations :whitespace
                        :pretty-print true
                        :preamble ["react/react_with_addons.js"]}}
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
