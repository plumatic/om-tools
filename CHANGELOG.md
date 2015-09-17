## 0.4.0

*   BREAKING: Upgrade dependency: schema 1.0.1, breaking compatibility with pre-1.0.0 schema
*   Upgrade dependency: plumbing 0.5.0

## 0.3.12

*   Upgrade dependency: schema 0.4.3
*   Upgrade dependency: plumbing 0.4.4

## 0.3.11

*   Upgrade dependency: schema 0.4.0
*   Upgrade dependency: plumbing 0.4.0

## 0.3.10

*   Generate functions that mirror DOM element macros

## 0.3.9

*   Added forward compatibility with Om 0.8.0-alpha5 (React 0.12.2)

## 0.3.8

Botched release. Don't use.

## 0.3.7

Botched release. Same as 0.3.6.

## 0.3.6

*   Upgrade dependency: schema 0.3.1
*   Upgrade dependency: plumbing 0.3.5

## 0.3.5

*   Removed org.clojure/clojure and com.keminglabs/cljx as dependencies

## 0.3.4

*   Fix exception thrown when defcomponent(k) form is invalid
*   Add `om-tools.core/defcomponentmethod` macro to register components in multimethod
*   Improve performance of components defined with `defcomponentk`
*   Enable Schema-style :always-validate and :never-validate metadata on
    defcomponentk

## 0.3.3

*   Automatically generate IDisplayName when not explicitly defined.

## 0.3.2

*   Fix DOM functions to properly handle non-literal nested attributes

## 0.3.0

*   Upgrade to Om 0.7.1
*   BREAKING: components with mixins generate React descriptor with "$descriptor"
    suffix rather than a React constructor with a "$ctor" suffix.

## 0.2.3

*   Change non-essential dependencies to be transient to avoid conflicting with
    user supplied dependencies.

## 0.2.2

*   Upgrade dependency: schema 0.2.4
*   Upgrade dependency: plumbing 0.3.2

## 0.2.1

*   Add `om-tools.mixin` namespace for defining Om mixins
*   Add `:mixin` option to `defcomponent`/`defcomponentk` macro to generate
    React component constructor with mixins configured.
*   Fixes `om-tools.dom` unparsed attribute values for maps.
*   Fixes `om-tools.dom` unwanted camelCasing of data-* and aria-* attributes.

## 0.2.0

*   Rename `defcomponent` macro to `defcomponentk` to indicate it uses
    `fnk`-style syntax and define `defcomponent` macro that uses normal
    `defn`-style arguments.
*   Add `om.core/set-state?!` function
*   Add `om.dom/class-set` function
*   Add `:shared` key option to `defcomponentk` for accessing shared
    Om data from `om.core/get-shared`.
*   Optimize `defcomponentk` macro to not create state proxy or shared
    data unless it is used.

## 0.1.1

*   Fix the need to use `#js` reader macro on :style attributes
