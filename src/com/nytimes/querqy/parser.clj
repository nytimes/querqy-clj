(ns com.nytimes.querqy.parser
  (:require
   [com.nytimes.querqy.protocols :as p])
  (:import
   (querqy.parser FieldAwareWhiteSpaceQuerqyParser QuerqyParser WhiteSpaceQuerqyParser)))

(def whitespace-parser
  (WhiteSpaceQuerqyParser.))

(def field-aware-whitespace-parser
  (FieldAwareWhiteSpaceQuerqyParser.))

(extend-protocol p/Parser
  QuerqyParser
  (p/parse [this string]
    (.parse this string)))
