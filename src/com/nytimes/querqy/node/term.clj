(ns com.nytimes.querqy.node.term
  (:require
   [com.nytimes.querqy.node.protocols :as node]))

(defrecord TermNode [fields text boost]
  node/Node
  (node-type [_] :term))

(defn term-node
  [& {:keys [fields text boost]}]
  (->TermNode fields text boost))
