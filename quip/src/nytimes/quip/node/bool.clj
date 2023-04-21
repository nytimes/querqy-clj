(ns nytimes.quip.node.bool
  (:require [nytimes.quip.node.protocols :as node]))

(defrecord Clause [type child]
  node/Node
  (tag [_] type)

  node/BranchNode
  (branch? [_] true)
  (children [_] (list child))
  (update [this children]
    (assert (= 1 (count children)))
    ;;(assert-child-count! children 1)
    (assoc this :child (first children))))

(defn clause?
  [obj]
  (instance? Clause obj))

(defn should
  [child]
  (->Clause :should child))

(defn must
  [child]
  (->Clause :must child))

(defn must-not
  [child]
  (->Clause :must-not child))

(defn filter
  [child]
  (->Clause :filter child))

(defrecord Bool [children]
  node/Node
  (tag [_] :bool)

  node/BranchNode
  (branch? [_] true)
  (children [_] children)
  (update [this children]
    (assoc this :children children)))

(defn bool
  [child & children]
  (let [clauses (seq (if (sequential? child) (into child children) (list* child children)))]
    (assert (every? clause? children) "all children of bool must be clauses")
    (->Bool clauses)))

(defn bool? [node] (= :bool (node/tag node)))
