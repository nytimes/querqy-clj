(ns nytimes.quip.node.term
  (:require
   [nytimes.quip.node.protocols :as node]
   [clojure.string :as str]))

(defrecord Term [text fields boost]
  node/Node
  (tag [_] :term))

(defn term
  [text & {:keys [fields boost] :or {boost 1.0}}]
  (->Term text fields boost))

(defn term?
  [obj]
  (instance? Term obj))

(defn text=
  [x y]
  (and (term? x) (term? y) (= (:text x) (:text y))))
