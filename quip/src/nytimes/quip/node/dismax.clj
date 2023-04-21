(ns nytimes.quip.node.dismax
  (:require
   [nytimes.quip.node.protocols :as node]
   [nytimes.quip.node.term :as term]))

(defrecord Dismax [children]
  node/Node
  (tag [_] :dismax)

  node/BranchNode
  (branch? [_] true)
  (children [_] children)
  (update [this children]
    (assoc this :children children)))

(defn dismax
  [& children]
  (assert (every? term/term? children))
  (->Dismax children))
