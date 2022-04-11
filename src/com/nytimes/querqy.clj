(ns com.nytimes.querqy
  "Functions to working with Querqy from Clojure. "
  (:require [com.nytimes.querqy]
            [com.nytimes.querqy.protocols :as p]
            [com.nytimes.querqy.parser :as parser]
            [com.nytimes.querqy.context :as context]
            [com.nytimes.querqy.elasticsearch :as elasticsearch])
  (:import (querqy.rewrite RewriteChain)
           (querqy.model ExpandedQuery)))

(def ^:dynamic *query-parser* parser/whitespace-parser)
(def ^:dynamic *query-emitter* (elasticsearch/elasticsearch-query-emitter))
(def ^:dynamic *query-context* context/empty-context)

(defn parse
  "Parse a query string into Querqy's ExpandedQuery representation."
  ([string]
   (ExpandedQuery. (p/parse *query-parser* string)))
  ([parser string]
   (ExpandedQuery. (p/parse parser string))))

(defn rewrite
  "Rewrite an ExpandedQuery using the given rewriter. Accepts an ExpandedQuery or a string.
   If given a string, it will use the `parse` function to turn the string into an
   ExpandedQuery."
  [rewriter query]
  (p/rewrite rewriter
             (if (string? query) (parse query) query)
             *query-context*))

(defn emit
  "Emit a system-specific query for an ExpandedQuery."
  ([query]
   (emit query nil))
  ([query opts]
   (p/emit *query-emitter* query opts)))

(defn chain-of
  "Compose multiple rewriters into a single rewriter. The rewriters will be applied in the order specified."
  [rewriters]
  (RewriteChain. rewriters))

(comment
  (require '[com.nytimes.querqy.replace :as rep])
  (require '[com.nytimes.querqy.rules :as rule])

  (def typos-rewriter
    (rep/replace-rewriter
      (rep/replace "ombiles" (rep/with "mobile"))
      (rep/replace "ihpone" (rep/with "iphone"))
      (rep/delete "cheap")))

  (def rules-rewriter
    (rule/rules-rewriter
      (rule/match (and "iphone" (not "case"))
                  (rule/boost 100 {:term {:category "mobiles"}}))))

  (def chain (chain-of [typos-rewriter rules-rewriter]))

  ;; rewriter chain will delete cheap, correct ihpone typo, and boost mobile phone category
  (emit (rewrite chain "cheap ihpone")
        {:match/fields ["title", "body"]})
  ;; =>
  {:function_score
   {:query
    {:bool {:must     [],
            :should   [{:dis_max
                        {:queries [{:match {"title" {:query "iphone"}}}
                                   {:match {"body" {:query "iphone"}}}]}}],
            :must_not [],
            :filter   []}},
    :functions [{:filter {:term {:category "mobiles"}}, :weight 100.0}]}}

  )
