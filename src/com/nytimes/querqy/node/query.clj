(ns com.nytimes.querqy.node.query
  (:require
   [com.nytimes.querqy.node.protocols :as node]))

(def query-types
  #{:query :filter :boost})

#_(defrecord QueryClauseNode [type query]
  node/Node
  (node-type [_] :query-clause)

  node/InnerNode
  (inner? [_] true)
  (children [_] (list query))
  (replace-children [node query]
    (assoc node :query query)))

(defrecord QueryNode [query filters boosts]
  node/Node
  (node-type [_] :query)

  node/InnerNode
  (inner? [_] true)
  (children [_] (concat (list query) filters boosts))
  (replace-children [node children]
    (assoc node :children children)))

#_(defn query-clause-node [type query]
  (->QueryClauseNode type query))

(defn query-node
  [& {:keys [query filters boosts] :or {filters [], boosts []}}]
  (->QueryNode query filters boosts))
