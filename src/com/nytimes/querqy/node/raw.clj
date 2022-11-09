(ns com.nytimes.querqy.node.raw
  (:require
   [com.nytimes.querqy.node.protocols :as node]))


(defrecord RawNode [query]
  node/Node
  (node-type [_] :raw))

(defn raw-node
  [query]
  (->RawNode query))
