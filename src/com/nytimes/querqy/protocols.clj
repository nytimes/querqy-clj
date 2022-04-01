(ns com.nytimes.querqy.protocols
  (:require
    [com.nytimes.querqy.context :as context])
  (:import
    (querqy.model ExpandedQuery)
    (querqy.rewrite ContextAwareQueryRewriter RewriteChain RewriterFactory SearchEngineRequestAdapter)))

(defprotocol Parser
  ;; returns Query, not Expanded Query
  (parse [this ^String string]))

;; ----------------------------------------------------------------------

(defprotocol Rewriter
  (rewrite
    [this
     ^ExpandedQuery query]
    [this
     ^ExpandedQuery query
     ^SearchEngineRequestAdapter context]))

(extend-protocol Rewriter
  ContextAwareQueryRewriter
  (rewrite
    ([this query]
     (rewrite this query context/empty-context))
    ([this query context]
     (.rewrite this query context)))

  RewriteChain
  (rewrite
    ([this query]
     (rewrite this query context/empty-context))
    ([this query context]
     (.rewrite this query context)))

  RewriterFactory
  (rewrite
    ([this query]
     (rewrite this query context/empty-context))
    ([this query context]
     (let [rewriter (.createRewriter this query context)]
       (rewrite rewriter query context)))))

;; ----------------------------------------------------------------------

(defprotocol Emitter
  (emit [this query opts]))
