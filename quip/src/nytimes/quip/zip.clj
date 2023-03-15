(ns nytimes.quip.zip
  "Zipper abstraction for query tree"
  (:refer-clojure :exclude [next remove replace find])
  (:require
   [clojure.zip :as cz]
   [nytimes.quip.node :as node]))

(defn zipper
  [root]
  (cz/zipper node/branch?
             (comp seq node/children)
             node/update
             root))

(defn next [loc] (cz/next loc))
(defn down [loc] (cz/down loc))
(defn branch? [loc] (cz/branch? loc))
(defn node [loc] (cz/node loc))
(defn append-child [loc item] (cz/append-child loc item))
(defn children [loc] (cz/children loc))
(defn edit [loc f & args] (apply cz/edit loc f args))
(defn end? [loc] (cz/end? loc))
(defn insert-child [loc item] (cz/insert-child loc item))
(defn insert-left [loc item] (cz/insert-left loc item))
(defn insert-right [loc item] (cz/insert-right loc item))
(defn left [loc] (cz/left loc))
(defn leftmost [loc] (cz/leftmost loc))
(defn lefts [loc] (cz/lefts loc))
(defn make-node [loc node children] (cz/make-node loc node children))
(defn path [loc] (cz/path loc))
(defn prev [loc] (cz/prev loc))
(defn remove [loc] (cz/remove loc))
(defn replace [loc node] (cz/replace loc node))
(defn right [loc] (cz/right loc))
(defn rightmost [loc] (cz/rightmost loc))
(defn rights [loc] (cz/rights loc))
(defn up [loc] (cz/up loc))
(defn root [loc] (cz/root loc))


(defn find
  ([loc p?]
   (find loc next p?))
  ([loc f p?]
   (->> loc
        (iterate f)
        (take-while identity)
        (take-while (complement end?))
        (drop-while (complement p?))
        (first))))


(defn find-node
  ([loc p?]
   (find-node loc next p?))
  ([loc f p?]
   (find loc f (comp p? node))))
