(ns com.nytimes.querqy
  (:require [com.nytimes.querqy]
            [com.nytimes.querqy.protocols :as p]
            [com.nytimes.querqy.parser :as parser]
            [com.nytimes.querqy.replace :as r])
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

  (require '[com.nytimes.querqy.replace :refer [replace-rewriter]])
  (require '[com.nytimes.querqy.datafy :refer [datafy-emitter]])
  (require '[com.nytimes.querqy.common :as cr])

  (def typos
    (replace-rewriter {"ombile" "mobile"}))

  (def useless-terms
    (replace-rewriter
      (into {} (map vector ["cheap" "fast"] (repeat "")))))

  (def chain (chain-of [typos useless-terms]))

  (binding [*query-emitter* datafy-emitter]
    (->> (parse "cheap ombile phones") (rewrite chain) (emit {})))

  )
