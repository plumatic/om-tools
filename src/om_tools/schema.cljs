(ns om-tools.schema
  (:require
   [schema.core :as s :include-macros true]
   [om.core :as om]))

(s/defschema Cursor (s/pred om/cursor?))

(defn cursor
  "Returns a schema to validate an Om cursor"
  [schema]
  (s/conditional om/cursor? schema))
