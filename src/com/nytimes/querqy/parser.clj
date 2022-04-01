(ns com.nytimes.querqy.parser
  (:require
    [clojure.string :as str]
    [com.nytimes.querqy.protocols :as p])
  (:import
    (querqy.parser QuerqyParser WhiteSpaceQuerqyParser FieldAwareWhiteSpaceQuerqyParser)
    (querqy.rewrite.commonrules LineParser)))

(def whitespace-parser
  (WhiteSpaceQuerqyParser.))

(def field-aware-whitespace-parser
  (FieldAwareWhiteSpaceQuerqyParser.))

(extend-protocol p/Parser
  QuerqyParser
  (p/parse [this string]
    (.parse this string)))


;; Other parsing adjacent functions

(defn parse-terms [string] (mapv #(LineParser/parseTerm %) (str/split string #"\s+")))
