(ns com.nytimes.querqy
  (:require [com.nytimes.querqy]
            [com.nytimes.querqy.protocols :as p]
            [com.nytimes.querqy.parser :as parser])
  (:import (querqy.rewrite RewriteChain)
           (querqy.model ExpandedQuery)))

(def ^:dynamic *query-parser* parser/whitespace-parser)
(def ^:dynamic *query-emitter* nil)

(defn parse
  ([string]
   (ExpandedQuery. (p/parse *query-parser* string)))
  ([parser string]
   (ExpandedQuery. (p/parse parser string))))

(defn rewrite
  [rewriter query]
  (p/rewrite rewriter (if (string? query) (parse query) query)))

(defn emit
  [opts query]
  (p/emit *query-emitter* query opts))

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
  (datafy (rewrite chain "cheap ihpone"))
  ;; =>
  {:type       querqy.model.ExpandedQuery,
   :user-query {:type    querqy.model.Query,
                :occur   :should,
                :clauses [{:type    querqy.model.DisjunctionMaxQuery,
                           :occur   :should,
                           :clauses [{:type querqy.model.Term, :field nil, :value "iphone"}]}]},
   :boost-up   [{:type  querqy.model.BoostQuery,
                 :boost 100.0,
                 :query {:type com.nytimes.querqy.model.RawQuery, :query {:term {:category "mobiles"}}}}],
   :boost-down [],
   :filter     []})
