(ns nytimes.quip.node.match-all
  (:require
   [nytimes.quip.node.protocols :as node]))

(defrecord MatchAll []
  node/Node
  (tag [_] :match-all))

(defn match-all
  []
  (->MatchAll))
