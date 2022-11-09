(ns com.nytimes.querqy.node.boost
  (:require
   [com.nytimes.querqy.node.protocols :as node]))

(defrecord BoostNode [boost query]
  node/Node
  (node-type [_] :boost)

  node/InnerNode
    (inner? [_] true)
    (children [_] (list query))
    (replace-children [node query]
      (assoc node :query query)))

(defn boost-node
  [boost query]
  (->BoostNode boost query))
