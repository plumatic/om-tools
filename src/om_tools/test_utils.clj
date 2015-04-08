(ns om-tools.test-utils)

(defmacro with-element
  [bindings & body]
  (let [bind-pairs (partition 2 bindings)
        el-syms (map first bind-pairs)]
    `(let [~@(mapcat (fn [[sym tag]]
                       [sym `(~'.createElement js/document ~tag)])
                     bind-pairs)]
       ~@(map (fn [sym] `(~'.appendChild (~'.-body js/document) ~sym))
              el-syms)
       ~@body
       ~@(map (fn [sym] `(~'.removeChild (~'.-body js/document) ~sym))
              el-syms))))
