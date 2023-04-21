(ns nytimes.quip.node.raw
  (:require [nytimes.quip.node.protocols :as node]))

(defrecord Raw [query]
  node/Node
  (tag [_] :raw))

(defn raw [query]
  (->Raw query))
