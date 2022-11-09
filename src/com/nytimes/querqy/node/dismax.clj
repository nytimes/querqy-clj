(ns com.nytimes.querqy.node.dismax
  (:require
   [com.nytimes.querqy.node.protocols :as node]))

(defrecord DismaxNode [terms]
  node/Node
  (node-type [_] :dismax)

  node/InnerNode
  (inner? [_] true)
  (children [_] terms)
  (replace-children [node terms]
    (assoc node :terms terms)))

(defn dismax-node [terms]
  (->DismaxNode terms))
