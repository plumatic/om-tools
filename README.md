# om-tools

A ClojureScript library of general-purpose tools for building applications with
[Om](https://github.com/omcljs/om) and
[Facebook's React](http://facebook.github.io/react/).

Leiningen dependency (Clojars):

[![Clojars Project](https://img.shields.io/clojars/v/prismatic/om-tools.svg)](https://clojars.org/prismatic/om-tools)

[![Build Status](https://travis-ci.org/plumatic/om-tools.svg?branch=fix-react-0-12-dom)](https://travis-ci.org/plumatic/om-tools)

**This library does not currently have an active maintainer.  If you are interested in becoming one, please post an issue.**

## Introduction

om-tools aims to provide higher-order abstractions and utilities frequently
useful when building components with Om's API.

## Contents

*   [DOM tools](#dom-tools)
*   [Components tools](#component-tools)
    *   [`defcomponent`](#defcomponent)
    *   [`defcomponentk`](#defcomponentk)
    *   [`defcomponentmethod`](#defcomponentmethod)
*   [Mixin tools](#mixin-tools)

## DOM tools

`om-tools.dom` mirrors the `om.dom` namespace while using macros and
minimal runtime overhead to make the following improvements:


*   Element attributes are not required to be JavaScript values and are
    optional. You don't need to use the `#js` reader macro or `nil`
    for no attributes.
*   More natural attribute names. We translate attributes like
    `:class` to `:className` and `:on-click` to `:onClick` to stay
    consistent with Clojure naming conventions.
*   Children can be in collections.  You don't need to use `apply` if
    you have a sequence of children or use `concat` for combining
    sequences of siblings.

Example by comparison. First with `om.dom`:

```clojure
(ns example
  (:require [om.dom :as dom :include-macros true]))

(dom/div
  nil
  (apply dom/ul #js {:className "a-list"}
         (for [i (range 10)]
           (dom/li #js {:style #js {:color "red"}}
                   (str "Item " i)))))
```

And with `om-tools.dom`:

```clojure
(ns example
  (:require [om-tools.dom :as dom :include-macros true]))

(dom/div
  (dom/ul {:class "a-list"}
          (for [i (range 10)]
            (dom/li {:style {:color "red"}}
                    (str "Item " i)))))
```

## Component tools

### `defcomponent`

The `om-tools.core/defcomponent` macro defines Om component
constructor functions.

Advantages over the ordinary `defn` & `reify` approach:

*   Removes boilerplate code around using `reify` to instantiate
    objects with Om lifecycle methods. Component definitions become
    much smaller and easier to read.
*   Adds [Schema][schema] support to specify and validate the data
    when component is built.

    One of React's most
    [powerful features](https://speakerdeck.com/vjeux/why-does-react-scale-jsconf-2014)
    is
    [prop validation](http://facebook.github.io/react/docs/reusable-components.html#prop-validation),
    which allows a component's author to document and validate which
    properties a component requires and their types.

    This functionality is not utilized in Om because we use normal
    ClojureScript data structures as component inputs. However, with
    more complex input structures, documentation and validation are
    even more important.

    Schema annotations are optional and validation is disabled by
    default.
*   Automatically implements `IDisplayName` for better debugging messages.

Example of `defcomponent` including schema annotation:

```clojure
(ns example
  (:require
    [om-tools.core :refer-macros [defcomponent]]
    [om-tools.dom :include-macros true]))

(defcomponent counter [data :- {:init js/Number} owner]
  (will-mount [_]
    (om/set-state! owner :n (:init data)))
  (render-state [_ {:keys [n]}]
    (dom/div
      (dom/span (str "Count: " n))
      (dom/button
        {:on-click #(om/set-state! owner :n (inc n))}
        "+")
      (dom/button
        {:on-click #(om/set-state! owner :n (dec n))}
        "-"))))

(om/root counter {:init 5}
         {:target (. js/document -body)})
```

### `defcomponentk`

The `om-tools.core/defcomponentk` macro is similar to `defcomponent`,
except that it uses [Plumbing][plumbing]'s `fnk` destructuring
syntax for constructor arguments.
This enables succinct and declaritive definition of the structure and
requirements of component input data.

It also provides additional useful utilities mentioned in
[Component Inputs](#component-inputs).

#### Fnk-style Arguments

The args vector of `defcomponentk` uses
[Fnk syntax](https://github.com/plumatic/plumbing/tree/master/src/plumbing/fnk#fnk-syntax)
that's optimized for destructuring (nested) maps with keyword keys.
It is the similar pattern used in our
[Fnhouse](https://github.com/plumatic/fnhouse) library to
expressively declare HTTP handlers.

If you are unfamiliar with this syntax, here are some quick comparisons
to default Clojure map destructuring.

```clojure
{:keys [foo bar]}                    :: [foo bar]
{:keys [foo bar] :as m}              :: [foo bar :as m]
{:keys [foo bar] :or {bar 21}}       :: [foo {bar 21}]
{{:keys [baz qux]} :foo :keys [bar]} :: [[:foo baz qux] bar]
```

However, an important distinction between Clojure's default
destructuring and Fnk-style is that specified keys are required by
default.
Rather than defaulting to `nil`, if a key that's destructured is
missing and no default value is specified, an error is thrown.

By being explicit about component inputs, we are less error-prone and
debugging is often easier because errors happen closer to the source.

#### Component Inputs

The map that's passed to `defcomponentk` arg vector has the following
keys:

| Key       | Description
| --------- |-------------------------------------------------------------------
| `:data`   | The data (cursor) passed to component when built
| `:owner`  | The backing React component
| `:opts`   | The optional map of options passed when built
| `:shared` | The map of globally shared data from om.core/get-shared
| `:state`  | An atom-like object for convenience to om.core/get-state and om.core/set-state!

#### Example

```clojure
(ns example
  (:require
    [om.core :as om]
    [om-tools.core :refer-macros [defcomponentk]]
    [schema.core :refer-macros [defschema]]))

(defschema ProgressBar
  {:value js/Number
   (s/optional-key :min) js/Number
   (s/optional-key :max) js/Number})

(defcomponentk progress-bar
  "A simple progress bar"
  [[:data value {min 0} {max 100}] :- ProgressBar owner]
  (render [_]
    (dom/div {:class "progress-bar"}
      (dom/span
        {:style {:width (-> (/ value (- max min))
                            (* 100)
                            (int)
                            (str "%"))}}))))
```

```clojure
;; Valid
(om/root progress-bar {:value 42}
  {:target (. js/document (getElementById "app"))})

;; Throws error: Key :value not found in (:wrong-data)
(om/root progress-bar {:wrong-data true}
  {:target (. js/document (getElementById "app"))})

;; Throws error: Value does not match schema
(schema.core/with-fn-validation
  (om/root progress-bar {:value "42"}
    {:target (. js/document (getElementById "app"))})
```

#### State Proxy (experimental)

A component using `defcomponentk` can use the key, `:state`, to access
an atom-like object that conveniently wraps `om.core/get-state` and
`om.core/set-state!` so that we can read and write state idiomatically
with `deref`, `reset!` and `swap!`.

```clojure
(defcomponentk progress-bar
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

It's important to note that while `state` looks and behaves like
an `atom`, there is at least one minor difference: changes made by
`swap!` and `reset!` are not immediately available if you `deref`
in the same render phase.

### `defcomponentmethod`

With Om, [multimethods](http://clojure.org/multimethods) can be used
instead of normal functions to create polymorphic components (requires
Om version 0.7.0+).
The `defcomponentmethod` macro allows you to register components into
a multimethod (created from `cljs.core/defmulti`), while using
the normal om-tools syntax.

```clojure
(defmulti fruit-basket-item
  (fn [fruit owner] (:type fruit)))

(defcomponentmethod fruit-basket-item :orange
  [orange owner]
  (render [_]
    (dom/label "Orange")))

(defcomponentmethod fruit-basket-item :banana
  [banana owner]
  (render [_]
    (dom/label
     {:class (when (:peeled? banana) "peeled")}
     "Banana")))

(defcomponentmethod fruit-basket-item :default
  [fruit owner]
  (render [_]
    (dom/label (str "Unknown fruit: " (name (:type fruit))))))

(om/build-all fruit-basket-item
              [{:type :banana}
               {:type :pineapple}
               {:type :orange}])
```

## Mixin tools

React provides [mixin functionality][react-mixin] to handle
cross-cutting concerns and allow highly reusable component behaviors.
While [mixins are possible with Om][om-mixin], it does not provide
much functionality to support this React feature.
One issue is that you must create a React constructor and specify it
each time the component is built.
This puts the responsibility of using mixins on both the component
(create a constructor) and its parent (specify the constructor).
Another issue is having to drop down to raw JavaScript functions,
breaking you out of Om's data and state abstractions.

om-tools provides a `defmixin` macro in the `om-tools.mixin` namespace
to define mixins. The syntax of `defmixin` follows same pattern as the
component macros.

One last thing: the factory functions created by
`defcomponent`/`defcomponentk` (ie `(->component-name data)`)
encapsulate any custom constructor automatically. So a parent
component no longer needs to be aware when a child uses mixins!

Here's how you could reimplement [React's mixin example][react-mixin]:

```clojure
(ns example
  (:require
    [om-tools.core :refer-macros [defcomponentk]]
    [om-tools.dom :as dom :include-macros true]
    [om-tools.mixin :refer-macros [defmixin]]))

(defmixin set-interval-mixin
  (will-mount [owner]
    (set! (. owner -intervals) #js []))
  (will-unmount [owner]
    (.. owner -intervals (map js/clearInterval)))
  (set-interval [owner f t]
    (.. owner -intervals (push (js/setInterval f t)))))

(defcomponentk tick-tock [owner state]
  (:mixins set-interval-mixin)
  (init-state [_]
    {:seconds 0})
  (did-mount [_]
    (.set-interval owner #(swap! state update-in [:seconds] inc) 1000))
  (render [_]
    (dom/p
      (str "React has been running for " (:seconds @state) " seconds."))))
```

See [example](examples/mixin) for full version.

## Community

Please feel free to open an
[issue on GitHub](https://github.com/plumatic/om-tools/issues/new)

For announcements of new releases, you can also follow on
[@PrismaticEng](http://twitter.com/prismaticeng) on Twitter.

We welcome contributions in the form of bug reports and pull requests;
please see CONTRIBUTING.md in the repo root for guidelines.

## License

Copyright (C) 2014 Prismatic and Contributors. Distributed under the Eclipse
Public License, the same as Clojure.

[schema]: https://github.com/plumatic/schema
[plumbing]: https://github.com/plumatic/plumbing
[om]: https://github.com/swannodette/om
[react-mixin]: http://facebook.github.io/react/docs/reusable-components.html#mixins
[om-mixin]: https://github.com/swannodette/om/blob/master/examples/mixins/src/core.cljs
