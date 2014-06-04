(ns om-tools.schema
  (:require-macros
   [schema.macros :as sm])
  (:require
   [schema.core :as s]
   [om.core :as om]))

(sm/defschema Cursor (s/pred om/cursor?))

(defn cursor
  "Returns a schema to validate an Om cursor"
  [schema]
  (s/both Cursor schema))
