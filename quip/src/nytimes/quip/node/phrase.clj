(ns nytimes.quip.node.phrase
  (:require [nytimes.quip.node.protocols :as node]))

(defrecord Phrase [text boost]
  node/Node
  (tag [_] :phrase))

(defn phrase [text & {:keys [boost] :or {boost 1.0}}]
  (->Phrase text boost))
