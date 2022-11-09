(ns com.nytimes.querqy.node.boolean
  (:require
   [com.nytimes.querqy.node.protocols :as node]))

(def occurs
  #{:should :must :must-not})

(defrecord ClauseNode [occur node]
  node/Node
  (node-type [_] :clause)

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

(defn clause-node
  [occur node]
  (->ClauseNode occur node))

(defn boolean-node
  ([] (->BooleanNode []))
  ([clauses]
   (->BooleanNode clauses)))

(defn should
  [node child]
  (update node :clauses conj (->ClauseNode :should child)))

(defn must
  [node child]
  (update node :clauses conj (->ClauseNode :must child)))

(defn must-not
  [node child]
  (update node :clauses conj (->ClauseNode :must-not child)))


