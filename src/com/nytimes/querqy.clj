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

(comment
  (require '[com.nytimes.querqy.replace :as rep])
  (require '[com.nytimes.querqy.commonrules :as rul])

  (def typos-rewriter
    (rep/replace-rewriter
      (rep/replace "ombiles" (rep/with "mobile"))
      (rep/replace "ihpone" (rep/with "iphone"))
      (rep/delete "cheap")))

  (def rules-rewriter
    (rul/rules-rewriter
      (rul/match (and "iphone" (not "case"))
                  (rul/boost 100 {:term {:category "mobiles"}}))))

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
