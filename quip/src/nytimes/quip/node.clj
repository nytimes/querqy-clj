(ns nytimes.quip.node)

(defprotocol Node
  (node-type [this])
  (branch? [this])
  (leaf? [this]))

(defprotocol BranchNode
  (children [node] [node children]))

(defprotocol LeafNode)

;; ----------------------------------------------------------------------
;; Nodes

(defrecord Term [fields text boost]
  Node
  (node-type [_] ::term)
  (branch? [_] false)
  (leaf? [_] true)

  LeafNode)

(defn term
  [& {:keys [fields text boost]}]
  (->Term fields text boost))

(defn term? [obj] (instance? Term obj))

;; Phrase

(defrecord Phrase [terms]
  Node
  (node-type [_] ::phrase)
  (branch? [_] true)
  (leaf? [_] false)

  BranchNode
  (children [_] terms)
  (children [node terms]
    (assoc node :terms terms)))

(defn phrase [terms]
  (->Phrase terms))

;; Dismax

(defrecord Dismax [terms]
  Node
  (node-type [_] ::dismax)
  (branch? [_] true)
  (leaf? [_] false)

  BranchNode
  (children [_] terms)
  (children [node terms]
    (assoc node :terms terms)))

(defn dismax [terms]
  (->Dismax terms))


;; Raw

(defrecord Raw [query]
  Node
  (node-type [_] ::raw)
  (branch? [_] false)
  (leaf? [_] true)
  LeafNode)

(defn raw [query]
  (->Raw query))

;; Boost

(defrecord Boost [boost query]
  Node
  (node-type [_] ::boost)
  (branch? [_] true)
  (leaf? [_] false)

  BranchNode
  (children [_] (list query))
  (children [node query]
    (assoc node :query query)))

(defn boost
  [boost query]
  (->Boost boost query))


;; Boolean

(def occurs
  #{:should :must :must-not})

(defrecord Clause [occur node]
  Node
  (node-type [_] ::clause)
  (branch? [_] true)
  (leaf? [_] false)

  BranchNode
  (children [_] (list node))
  (children [node node']
    (assoc node :node node')))

(defn clause
  [occur node]
  (->Clause occur node))

(defrecord Bool [clauses]
  Node
  (node-type [_] ::bool)
  (branch? [_] true)
  (leaf? [_] false)

  BranchNode
  (children [_] clauses)
  (children [node clauses]
    (assoc node :clauses clauses)))

(defn bool
  ([] (->Bool []))
  ([clauses]
   (->Bool clauses)))

;; Query

(def subquery-types
  #{:query :filter :boost})

(defrecord Subquery [type query]
  Node
  (node-type [_] ::subquery)
  (branch? [_] true)
  (leaf? [_] false)

  BranchNode
  (children [_] (list query))
  (children [node query]
    (assoc node :query query)))

(defn subquery
  [type query]
  (->Subquery type query))

(defrecord Query [query filters boosts]
  Node
  (node-type [_] ::query)
  (branch? [_] true)
  (leaf? [_] false)

  BranchNode
  (children [_] (concat (list query) filters boosts))
  (children [node children]
    (assoc node :children children)))

(defn query
  [& {:keys [query filters boosts] :or {filters [], boosts []}}]
  (->Query query filters boosts))
