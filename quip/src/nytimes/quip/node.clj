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

(defn term= [x y]
  (and (term? x) (term? y) (= (:text x) (:text y))))

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
  (tag [_] type)

  BranchNode
  (branch? [_] true)
  (children [_] children)
  (update [this children]
    (assert-child-count! children 1)
    (assoc this :children children)))

(defn should
  [children]
  (->Clause :bool/should (if (sequential? children) children (list children))))

(defn must
  [children]
  (->Clause :bool/must (if (sequential? children) children (list children))))

(defn must-not
  [children]
  (->Clause :bool/must-not (if (sequential? children) children (list children))))

(defn filter
  [children]
  (->Clause :bool/filter (if (sequential? children) children (list children))))

(defrecord Bool [children]
  Node
  (tag [_] :bool)

  BranchNode
  (branch? [_] true)
  (children [_] children)
  (update [this children]
    (assoc this :children children)))

(defn bool
  [children]
  (->Bool (if (sequential? children) children (list children))))

(defn bool? [node] (= :bool (tag node)))

;; Query

;; TODO: Think a bit about this top-level structure

(defrecord Query [children]
  Node
  (tag [_] :query)

  BranchNode
  (branch? [_] true)
  (children [_] children)
  (update [this children]
    (assoc this :children children)))

(defn boost
  [children]
  (->Clause :boost (if (sequential? children) children (list children))))

(defn query
  [children]
  (->Query (if (sequential? children) children (list children))))
