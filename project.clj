(defproject prismatic/om-tools "0.5.1-SNAPSHOT"
  :description "Tools for building Om applications"
  :url "http://github.com/plumatic/om-tools"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}

  :dependencies [[prismatic/plumbing "0.5.0"]
                 [prismatic/schema "1.0.1"]
                 [org.clojure/clojurescript "1.9.473" :scope "provided"]
                 [org.omcljs/om "1.0.0-beta1"]]

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.8.0"]
                                  [com.cemerick/clojurescript.test "0.3.3"]]
                   :plugins [[lein-cljsbuild "1.1.7"]]}}

  :aliases {"deploy" ["do" "clean," "deploy" "clojars"]
            "test" ["do" "clean," "test," "with-profile" "+dev" "cljsbuild" "test"]}

  :jar-exclusions [#"\.swp|\.swo|\.DS_Store"]

  :lein-release {:deploy-via :shell
                 :shell ["lein" "deploy"]}

  :source-paths ["src"]
  :cljsbuild
  {;;:test-commands
   #_{"unit" ["phantomjs"
              "test/vendor/es5-shim.js"
              "test/vendor/es5-sham.js"
              "test/vendor/console-polyfill.js"
              "target/test.js"]}
   :builds [{:id "test"
             :source-paths ["src" "test"]
             :compiler {:output-to "target/test.js"
                        :optimizations :advanced
                        :main "om-tools.test-runner"
                        :pretty-print true
                        :preamble ["react/react_with_addons.js"]}}
            {:id "example/sliders"
             :source-paths ["src" "examples/sliders/src"]
             :compiler {:output-to "examples/sliders/main.js"
                        :output-dir "examples/sliders/out"
                        :optimizations :none}}
            {:id "example/mixin"
             :source-paths ["src" "examples/mixin/src"]
             :compiler {:output-to "examples/mixin/main.js"
                        :output-dir "examples/mixin/out"
                        :optimizations :none}}]})
