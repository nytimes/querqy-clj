(ns com.nytimes.querqy.parser.simple
  "A simple query parser."
  (:require
   [clojure.string :as str]
   [com.nytimes.querqy.node.boolean :refer [boolean-node should]]
   [com.nytimes.querqy.node.query :refer [query-node]]
   [com.nytimes.querqy.node.term :refer [term-node]]))

(defn parse-query
  [string]
  (let [terms (-> (str/lower-case string) (str/split #"\s+"))]
    (query-node
     :query (boolean-node 
             (for [term terms]
               (should (term-node :text term)))))))
