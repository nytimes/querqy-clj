(ns nytimes.quip.node.protocols
  (:refer-clojure :rename {filter cfilter} :exclude [update]))

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
  (children [_] (throw (ex-info "unsupported operation" {})))
  (update [_] (throw (ex-info "unsupported operation" {}))))

(defn node? [obj]
  (and (some? obj) (not= :unknown (tag obj))))

(defn leaf? [obj]
  (and (node? obj) (not (branch? obj))))
