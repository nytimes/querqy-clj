(ns nytimes.quip.zip
  "Zipper abstraction for query tree"
  (:refer-clojure :exclude [next remove replace find])
  (:require
   [clojure.zip :as cz]
   [nytimes.quip.node :as node]))

(defn zipper
  [root]
  (cz/zipper
   node/branch?
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


#_(defn remove
  "Removes the node at loc, returning the loc that would have preceded
  it in a depth-first walk."
  {:added "1.0"}
  [loc]
    (let [[node {l :l, ppath :ppath, pnodes :pnodes, rs :r, :as path}] loc]
      (if (nil? path)
        (throw (new Exception "Remove at top"))
        (if (pos? (count l))
          (loop [loc (with-meta [(peek l) (assoc path :l (pop l) :changed? true)] (meta loc))]
            (if-let [child (and (branch? loc) (down loc))]
              (recur (rightmost child))
              loc))
          (with-meta [(make-node loc (peek pnodes) rs)
                      (and ppath (assoc ppath :changed? true))]
            (meta loc))))))

(defn delete
  "Like remove, but will continually remove locs that have no children."
  [loc])

;;

(defn tag
  [zloc]
  (some-> zloc cz/node node/tag))

(defn value
  [zloc]
  (some-> zloc cz/node))


;;

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

;; TODO

(defn find-node
  ([loc p?]
   (find-node loc next p?))
  ([loc f p?]
   (find loc f (comp p? node))))

(defn find-next
  ([zloc p?]
   (find-next zloc next p?))
  ([zloc f p?]
   (some-> zloc f (find f p?))))

(defn edit-all
  [zloc p? f]
  (loop [zloc (if (p? zloc) (f zloc) zloc)]
    (if-let [zloc (find-next zloc next p?)]
      (recur (f zloc))
      zloc)))

(defn find-all [zloc p?]
  (loop [matches [], zloc zloc]
    (prn (find-next zloc p?))
    (if-let [zloc (find-next zloc p?)]
      (recur (conj matches zloc)
             (next zloc))
      matches)))
