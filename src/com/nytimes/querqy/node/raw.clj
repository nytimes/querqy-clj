(ns com.nytimes.querqy.node.raw
  (:require
   [com.nytimes.querqy.node.protocols :as node]))

(defrecord RawNode [query]
  node/Node
  (node-type [_] :raw)

  node/InnerNode
  (inner? [_] true)
  (children [_] (list query))
  (replace-children [node query]
    (assoc node :query query)))

(defn raw-node
  [query]
  (->RawNode query))
