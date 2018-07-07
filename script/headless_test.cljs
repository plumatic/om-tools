(ns om-tools.headless-test
  "Head less chromium test runner lumo script using puppeteer.
   Does the following:
   1. Launches an instance of chromium
   2. Opens the test/index.html which in turn runs the test code
   3. Every log event is detected and kept track in an atom
   4. The watch on the above atom compares the state with expected-outputs and exits the process with status 0 if they match
   5. Otherwise process blocks until the process exits with status 1 via the timeout.
   ")

(def path (js/require "path"))

(def p (js/require "puppeteer"))

(def expected-outputs
  {"\nTesting om-tools.core-test " 1
   "\nTesting om-tools.dom-test " 1
   "\nTesting om-tools.mixin-test " 1
   "\nTesting om-tools.schema-test " 1
   "\nRan 13 synchronous tests containing 367 assertions." 1
   "Waiting on 1 asynchronous test to complete." 1
   "\nTesting om-tools.core-test (async)" 1
   "\nRan 14  tests containing 370 assertions." 1
   "Testing complete: 0 failures, 0 errors." 2})

(defn goto-test-html
  [p]
  (.goto p (str "file://" (path.resolve "test/index.html"))))

(defn watch-for-log-events [p result-store]
  (.on p "console"
       (fn [console-message]
         (swap! result-store
                update (.text console-message) inc))))

(js/setTimeout
 (fn []
   (js/console.error "Tests did not pass within allowed time")
   (.exit js/process 1))
 20000)

(let [browser (.launch p (clj->js
                          ;; Option required to run on travis
                          {:args ["--no-sandbox"]}))
      page (.then browser (fn [browser]
                            (.newPage browser)))
      result-store (let [a (atom {})]
                     (add-watch a ::store
                                (fn [_ _ _ n]
                                  (when (= n expected-outputs)
                                    (prn "Tests passed :)")
                                    (.exit js/process)))))]
  (-> page
      (.then #(watch-for-log-events % result-store))
      (.then goto-test-html)))
