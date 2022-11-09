(ns com.nytimes.querqy.node.query
  (:require
   [com.nytimes.querqy.node.protocols :as node]))


(defrecord QueryNode [query filters boosts]
  node/Node
  (node-type [_] :query))
