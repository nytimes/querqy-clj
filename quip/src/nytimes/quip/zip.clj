(ns nytimes.quip.zip
  "Zipper abstraction for query tree"
  (:refer-clojure :exclude [next remove replace find])
  (:require
   [clojure.zip :as cz]
   [nytimes.quip.node.protocols :as node]))

(defn zipper
  [root]
  (cz/zipper
   node/branch?
   (comp seq node/children)
   node/update
   root))

;; ----------------------------------------------------------------------
;; Base

(defn node
  [zloc]
  (cz/node zloc))

(defn make-node
  [zloc node children]
  (cz/make-node zloc node children))

(defn children
  [zloc]
  (cz/children zloc))

(defn tag
  [zloc]
  (some-> zloc node node/tag))

(defn branch?
  [zloc]
  (cz/branch? zloc))

(defn end?
  [zloc]
  (cz/end? zloc))

(defn root [loc] (cz/root loc))

(defn lefts [loc] (cz/lefts loc))

(defn path [loc] (cz/path loc))

(defn rights [loc] (cz/rights loc))

;; ----------------------------------------------------------------------
;; Movement

(defn next [loc] (cz/next loc))

(defn down [loc] (cz/down loc))

(defn left [loc] (cz/left loc))

(defn leftmost [loc] (cz/leftmost loc))

(defn prev [loc] (cz/prev loc))

(defn up [loc] (cz/up loc))

(defn right [loc] (cz/right loc))

(defn rightmost [loc] (cz/rightmost loc))

;; ----------------------------------------------------------------------
;; Finding

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

(defn find-next
  ([zloc p?]
   (find-next zloc next p?))
  ([zloc f p?]
   (some-> zloc f (find f p?))))

(defn find-all
  [zloc p?]
  (loop [matches [], zloc zloc]
    (if-let [zloc (find-next zloc p?)]
      (recur (conj matches zloc)
             (next zloc))
      matches)))

;; ----------------------------------------------------------------------
;; Editing

(defn append-child
  [loc item]
  (cz/append-child loc item))

(defn insert-child
  [loc item]
  (cz/insert-child loc item))

(defn insert-left
  [loc item]
  (cz/insert-left loc item))

(defn insert-right
  [loc item]
  (cz/insert-right loc item))

(defn edit
  [loc f & args]
  (apply cz/edit loc f args))

(defn edit-all
  ([zloc p? xf]
   (edit-all zloc p? next xf))
  ([zloc p? f xf]
   (loop [zloc (if (p? zloc) (edit zloc xf) zloc)]
     (if-let [zloc (find-next zloc f p?)]
       (recur (edit zloc xf))
       zloc))))

(defn remove
  [zloc]
  (cz/remove zloc))

(defn replace
  [zloc node]
  (cz/replace zloc node))
