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
