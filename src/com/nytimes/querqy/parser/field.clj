(ns com.nytimes.querqy.parser.field
  "A field-aware query parser."
  (:require
   [clojure.string :as str]
   [com.nytimes.querqy.node.boolean :refer [boolean-node should]]
   [com.nytimes.querqy.node.query :refer [query-node]]
   [com.nytimes.querqy.node.term :refer [term-node]]))

(defn parse-fields
  [string]
  (let [parsed (str/split string #":")]
    (cond
      (= 1 (count parsed))
      (vector nil (first parsed))

      (= 2 (count parsed))
      (vector (str/split (first parsed) #",")
              (second parsed))

      :else
      (throw (ex-info "Could not parse fields" {:input string})))))

(defn parse-query
  [string]
  (let [terms (-> (str/lower-case string) (str/split #"\s+"))]
    (query-node
     :query (boolean-node 
             (for [term terms]
               (let [[fields text] (parse-fields term)]
                 (should (term-node :fields fields, :text text))))))))

