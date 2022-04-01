(ns com.nytimes.querqy
  "Functions to working with Querqy from Clojure. "
  (:require
   [com.nytimes.querqy]
   [com.nytimes.querqy.context :as context]
   [com.nytimes.querqy.elasticsearch :as elasticsearch]
   [com.nytimes.querqy.parser :as parser]
   [com.nytimes.querqy.protocols :as p]
   [clojure.string :as str])
  (:import
   (querqy.model ExpandedQuery MatchAllQuery)
   (querqy.rewrite RewriteChain)))

(def ^:dynamic *query-parser* parser/whitespace-parser)
(def ^:dynamic *query-emitter* (elasticsearch/elasticsearch-query-emitter))
(def ^:dynamic *query-context* context/empty-context)

(defn parse
  "Parse a query string into Querqy's ExpandedQuery representation. An empty string will match all documents."
  ([input]
   (parse *query-parser* input))
  ([parser input]
   {:pre [(or (nil? input) (string? input))]}
   (cond (nil? input)       (ExpandedQuery. (MatchAllQuery.))
         (str/blank? input) (ExpandedQuery. (MatchAllQuery.))
         :else              (ExpandedQuery. (p/parse parser input)))))

(defn rewrite
  "Rewrite an ExpandedQuery using the given rewriter. Accepts an ExpandedQuery or a string or nil.
   If given a string, it will use the `parse` function to turn the string into an
   ExpandedQuery."
  [rewriter query]
  {:pre [(or (nil? query) (string? query) (instance? ExpandedQuery query))]}
  (p/rewrite rewriter
             (if (instance? ExpandedQuery query) query (parse query))
             *query-context*))

(defn emit
  "Emit a system-specific query for an ExpandedQuery."
  ([query]
   (emit query nil))
  ([query opts]
   {:pre [(instance? ExpandedQuery query)]}
   (p/emit *query-emitter* query opts)))

(defn chain-of
  "Compose multiple rewriters into a single rewriter. The rewriters will be applied in the order specified."
  [rewriters]
  (RewriteChain. rewriters))
