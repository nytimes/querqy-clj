(ns com.nytimes.querqy
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
  ([string]
   (ExpandedQuery. (p/parse *query-parser* string)))
  ([parser string]
   (ExpandedQuery. (p/parse parser string))))

(defn rewrite
  [rewriter query]
  (p/rewrite rewriter
             (if (string? query) (parse query) query)
             *query-context*))

(defn emit
  ([query]
   (emit query nil))
  ([query opts]
   (p/emit *query-emitter* query opts)))

(defn chain-of
  [rewriters]
  (RewriteChain. rewriters))

(comment
  (require '[com.nytimes.querqy.replace :as rep])
  (require '[com.nytimes.querqy.rules :as rule])
  (require '[clojure.datafy :refer [datafy]])

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
