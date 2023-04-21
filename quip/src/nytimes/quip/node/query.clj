(ns nytimes.quip.node.query
  (:require [nytimes.quip.node.protocols :as node]))

(defrecord Clause [type children]
  node/Node
  (tag [_] type)

  node/BranchNode
  (branch? [_] true)
  (children [_] children)
  (update [this children]
    (assoc this :children children)))

(defrecord Query [children]
  node/Node
  (tag [_] :query)

  node/BranchNode
  (branch? [_] true)
  (children [_] children)
  (update [this children]
    (assoc this :children children)))

(defn query
  [child & children]
  (let [clauses (seq (if (sequential? child) (into child children) (list* child children)))]
    (->Query clauses)))
