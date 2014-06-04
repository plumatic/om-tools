# om-tools

A ClojureScript library of general-purpose tools for building applications with
[om](https://github.com/swannodette/om) and [Facebook's React](http://facebook.github.io/react/)

Leiningen dependency (Clojars): `[prismatic/om-tools "0.1.0"]`

**This is an alpha release. The API and organizational structure are
  subject to change. Comments and contributions are much appreciated.**

## Introduction

We wrote this library to avoid boilerplate and build higher-level
abstractions on top of the Om's API.

### `component`

To illustrate our `component` macro, let's take a look at a very basic
vanilla Om component:

```clojure
(ns om-tools.readme
  (:require
    [om.core :as om]
    [om.dom :as dom :include-macros true]))

(defn welcome-component [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:some-state 1})
    om/IRender
    (render [this]
      (dom/div #js {:className "welcome-msg"} "Welcome!"))))
```

This is fairly simple, however as components get more complex and
more components are introduced to the application, certain parts of
this pattern get reptitive and distracting. Namely, `reify`ing a new
object and specifying protocol/method pairs.

Using the `component` macro, we can simply remove the reify and infer
the protocols from the method names. This is simple syntatic sugar
that can go a long way to improve readability and reduce code size.

```clojure
(ns om-tools.readme
  (:require
    [om-tools.core :refer [component] :include-macros true]
    [om-tools.dom :as dom :include-macros true]))

(defn welcome-component [data owner]
  (component
    (init-state [this]
      {:some-state 1})
    (render [this]
      (dom/div {:class "welcome-msg"} "Welcome!"))))
```

### `om-tools.dom`

Also displayed in the previous example was the `om-tools.dom`
namespace.

We built this to refine a few simple aspects of the base `om.dom`
elements. The main improvements include,

*   Attributes are optional not required to be JavaScript values.
    You don't need to use the `#js` reader macro or `nil` for no
    attributes.
*   More natural attribute names. We translate attributes like
    `:class` to `:className` and `:on-click` to `:onClick`.
*   Children can be in collections. Not examplified here, but we have
    removed the need to `apply` a DOM function to its (flattened)
    children.

### `defcomponent`

The `defcomponent` macro builds upon the `component` macro by adding,

*   Powerful map destructuring via our
    [Plumbing](https://github.com/Prismatic/plumbing) library
*   [Schema](https://github.com/Prismatic/schema) support
*   Access to higher level abstractions, like a
    [state proxy](#state-proxy)

You can find some [examples here](examples).

### Fnk-style Arguments

The args vector of `(defcomponent component-name [args*] body)`,
uses the
[Fnk syntax](https://github.com/Prismatic/plumbing/tree/master/src/plumbing/fnk#fnk-syntax)
that's optimized for destructuring (nested) maps with keyword keys.
It is the same pattern used in our
[Fnhouse](https://github.com/Prismatic/fnhouse) library to
expressively declare HTTP handlers.
Do not worry if you are unfamiliar with it; it's similar to
ordinary Clojure map destructuring, but more concise for nested maps
and specifying default values.

Here's a small comparison to default Clojure map destructuring of 4
keys, `k1`,`k2`,`k4` and `k5` where `k5` defaults to 5, and aliasing
the map to `m`:

```clojure
  Input: {:k1 1 :k2 2 :k3 {:k4 4}}
Default: {:keys [k1 k2] {:keys [k4 k5] :or {k5 5}} :k3 :as m}
    Fnk: [k1 k2 [:k3 k4 {k5 5}] :as m]
```

Another quick example:

```clojure
(use 'plumbing.core)

(defnk foo [x y] (+ x y))

(foo {:x 4 :y 2}) ;; => 6

(defnk bar [[:x a b] [:y c {d 0}]]
  (+ a b c d))

(bar {:x {:a 3 :b 4} :y {:c 5}}) ;; => 12
(bar {:x {:a 3 :b 4} :y {:c 5 :d -12}}) ;; => 0
```

An important distinction between Clojure's default destructuring
and Fnk-style is that specified keys are required by default.
Rather than defaulting to `nil`, if a key that's destructured is
missing and no default value is specified, an error is thrown.

```clojure
;; Throws error: missing y
(foo {:x 1})
```

By being explicit about your component's inputs, you are less
error-prone and debugging is often easier because errors happen
at the source.

### Component Inputs

The map that's passed to `defcomponent` arg vector has the following keys:

*   `:data`  The data (cursor) passed to component when built
*   `:owner` The backing React component
*   `:opts`  The optional map of options passed when built
*   `:state` An atom-like object for convenience to om.core/get-state and om.core/set-state!

#### Example

```clojure
(defcomponent progress-bar
  "A simple progress bar"
  [[:data value {min 0} {max 100}] owner]
  (render [_]
    (dom/div {:class "progress-bar"}
      (dom/span
        {:style {:width (-> (/ value (- max min))
                            (* 100)
                            (int)
                            (str "%"))}}))))

```

### Schema Support

One of React's most
[powerful features](https://speakerdeck.com/vjeux/why-does-react-scale-jsconf-2014)
is
[prop validation](http://facebook.github.io/react/docs/reusable-components.html#prop-validation),
which allows a component's author to document and enforce which properties
a component requires and their types.
However, this functionality is not utilized in Om because we use
normal ClojureScript data structures instead of properties.

`defcomponent` returns this functionality via
[Plumbing's](https://github.com/Prismatic/plumbing)
[Schema](https://github.com/Prismatic/schema) support.

```clojure
(require '[schema.core :as s])

(defschema ProgressBar
  {:value s/Num
   (s/optional-key :min) s/Num
   (s/optional-key :max) s/Num})

(defcomponent progress-bar
  "A simple progress bar"
  [[:data value {min 0} {max 100}] :- ProgressBar owner]
  (render [_]
    (dom/div {:class "progress-bar"}
      (dom/span
        {:style {:width (-> (/ value (- max min))
                            (* 100)
                            (int)
                            (str "%"))}}))))

;; Throws error:
(schema.macros/with-fn-validation
  (om/build progress-bar {:total 100 :value nil}))
```

### State Proxy (experimental)

A component can use the key, `:state`, to access an atom-like object
that conveniently wraps `om.core/get-state` and `om.core/set-state!`.

```clojure
(defcomponent progress-bar
  "A simple progress bar"
  [[:data value {min 0} {max 100}] state]
  (render [_]
    (dom/div {:class "progress-bar"}
      (dom/span
        {:style {:width (-> (/ value (- max min))
                            (* 100)
                            (int)
                            (str "%"))}
         :on-mouse-enter #(swap! state assoc :show-value? true)
         :on-mouse-leave #(swap! state assoc :show-value? false))}
        (when (:show-value? @state)
          (str value "/" total))))))
```

## Community

Please feel free to open an
[issue on GitHub](https://github.com/Prismatic/om-tools/issues/new)

For announcements of new releases, you can also follow on
[@PrismaticEng](http://twitter.com/prismaticeng) on Twitter.

We welcome contributions in the form of bug reports and pull requests;
please see CONTRIBUTING.md in the repo root for guidelines.

## License

Copyright (C) 2014 Prismatic and Contributors. Distributed under the Eclipse
Public License, the same as Clojure.
