(ns com.nytimes.querqy.node.boolean
  (:require
   [com.nytimes.querqy.node.protocols :as node]))

(def occurs
  #{:should :must :must-not})

(defrecord BooleanClauseNode [occur node]
  node/Node
  (node-type [_] :boolean-clause)

  node/InnerNode
  (inner? [_] true)
  (children [_] (list node))
  (replace-children [node node']
    (assoc node :node node')))

(defrecord BooleanNode [clauses]
  node/Node
  (node-type [_] :boolean)

  node/InnerNode
  (inner? [_] true)
  (children [_] clauses)
  (replace-children [node clauses]
    (assoc node :clauses clauses)))

(defn boolean-clause-node
  [occur node]
  (->BooleanClauseNode occur node))

(defn boolean-node
  ([] (->BooleanNode []))
  ([clauses]
   (->BooleanNode clauses)))

(defn add-clause
  [node clause]
  (update node :clauses conj clause))

(defn should
  [child]
  (->BooleanClauseNode :should child))

(defn must
  [child]
  (->BooleanClauseNode :must child))

(defn must-not
  [child]
  (->BooleanClauseNode :must-not child))


