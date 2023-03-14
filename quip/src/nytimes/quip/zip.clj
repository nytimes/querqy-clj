(ns nytimes.quip.zip
  "Zipper abstract for query tree"
  (:require
   [clojure.zip :as cz]
   [nytimes.quip.node :as node]))

(defn zipper
  [root]
  (cz/zipper node/branch?
             (comp seq node/children)
             node/children
             root))
