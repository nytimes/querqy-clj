(ns nytimes.quip.node
  (:refer-clojure :rename {filter cfilter} :exclude [update]))

(defn not-supported! []
  (throw (ex-info "unsupported operation" {})))

(defn assert-child-count!
  [children size]
  (assert (= size (count children))
          (format "expected %d %s" size (if (= size 1) "child" "children"))))

;; ----------------------------------------------------------------------

(defprotocol Node
  (tag [this]))

(defprotocol BranchNode
  (branch? [this])
  (children [this])
  (update [this children]))

(extend-type Object
  Node
  (tag [_] :unknown)
  BranchNode
  (branch? [_] false)
  (children [_] (not-supported!))
  (update [_] (not-supported!)))

(defn node? [obj]
  (and (some? obj) (not= :unknown (tag obj))))

(defn leaf? [obj]
  (and (node? obj) (not (branch? obj))))

;; ----------------------------------------------------------------------
;; Nodes

(defrecord Term [text fields boost]
  Node
  (tag [_] :term))

(defn term
  [text & {:keys [fields boost]}]
  (->Term text (or fields (list)) (or boost 1.0)))

(defn term? [obj] (= :term (tag obj)))

;; Phrase

(defrecord Phrase [text boost]
  Node
  (tag [_] :phrase))

(defn phrase [text & {:keys [boost]}]
  (->Phrase text (or boost 1.0)))

;; Dismax

(defrecord Dismax [children]
  Node
  (tag [_] :dismax)

  BranchNode
  (branch? [_] true)
  (children [_] children)
  (update [this children]
    (assoc this :children children)))

(defn dismax [children]
  (if (sequential? children)
    (->Dismax children)
    (->Dismax (list children))))


;; Raw

(defrecord Raw [query]
  Node
  (tag [_] :raw))

(defn raw [query]
  (->Raw query))

;; Boolean

(defrecord Clause [type children]
  Node
  (tag [_] (keyword "boolean" (name type)))

  BranchNode
  (branch? [_] true)
  (children [_] children)
  (update [this children]
    (assert-child-count! children 1)
    (assoc this :children children)))

(defn should
  [children]
  (->Clause :should (if (sequential? children) children (list children))))

(defn must
  [children]
  (->Clause :must (if (sequential? children) children (list children))))

(defn must-not
  [children]
  (->Clause :must-not (if (sequential? children) children (list children))))

(defn filter
  [children]
  (->Clause :filter (if (sequential? children) children (list children))))

#_(defrecord Should [node]
  Node
  (node-type [_] ::should)
  (branch? [_] true)
  (leaf? [_] false)

  BranchNode
  (children [_] (list node))
  (update [this node]
    (assoc this :node node)
    #_(prn :>> node)
    #_(assoc this :node (if (list? node)
                        (first node)
                        node))))

#_(defn should [node] (->Should node))

#_(defrecord Must [node]
  Node
  (node-type [_] ::must)
  (branch? [_] true)
  (leaf? [_] false)

  BranchNode
  (children [_] (list node))
  (update [node node'] (assoc node :node node')))

#_(defn must [node] (->Must node))

#_(defrecord MustNot [node]
  Node
  (node-type [_] ::must-not)
  (branch? [_] true)
  (leaf? [_] false)

  BranchNode
  (children [_] (list node))
  (update [node node'] (assoc node :node node')))

#_(defn must-not [node] (->MustNot node))

#_(defrecord Filter [node]
  Node
  (node-type [_] ::filter)
  (branch? [_] true)
  (leaf? [_] false)
  BranchNode
  (children [_] (list node))
  (update [this node] (assoc this :node node)))

#_(defn filter [node]
  (->Filter node))

#_(defrecord Bool [clauses]
  Node
  (node-type [_] ::bool)
  (branch? [_] true)
  (leaf? [_] false)

  BranchNode
  (children [_] clauses)
  (update [node clauses]
    (assoc node :clauses clauses)))

#_(defn bool
  ([] (->Bool []))
  ([clauses]
   (->Bool clauses)))

#_(defn bool? [node] (instance? Bool node))

;; Query

#_(defrecord Boost [node amount]
  Node
  (node-type [_] ::boost)
  (branch? [_] true)
  (leaf? [_] false)
  BranchNode
  (children [_] (list node))
  (update [this node] (assoc this :node node)))

#_(defn boost
  [node amount]
  (->Boost node amount))

#_(defn boost? [obj] (instance? Boost obj))

#_(defrecord Query [bool boosts]
  Node
  (node-type [_] ::query)
  (branch? [_] true)
  (leaf? [_] false)

  BranchNode
  (children [_] (cons bool boosts))
  (update [this nodes]
    (assoc this
           :bool (first (cfilter bool? nodes))
           :boosts (cfilter boost? nodes))))

(defn query
  [nodes]
  (->Query (first (cfilter bool? nodes))
           (cfilter boost? nodes)))




;; Helpers
