(ns com.nytimes.querqy.trie
  (:require
   [clojure.string :as str])
  (:import
   (clojure.lang Associative ILookup IPersistentCollection Counted MapEntry)
   (java.io Writer)))

(declare trie)
(declare get-trie)

#_(defn- keyseq [^String string]
  (map #(Character/toLowerCase ^Character %) string))


(deftype Trie [key val children]
  Associative
  (containsKey [this ks]
    (boolean (get this ks)))

  (entryAt [this ks]
    (MapEntry. ks (get this ks)))

  (assoc [this ks new-val]
    (if (empty? ks)
      (->Trie key new-val children)
      (->Trie key val (update children
                              (first ks)
                              (fnil assoc (->Trie (first ks) nil {}))
                              (rest ks)
                              new-val))))

  ;; ----------------------------------------------------------------------

  ILookup
  (valAt [this ks]
    (if-let [trie (get-trie this ks)]
      (.-val ^Trie trie)
      nil))

  (valAt [this ks not-found]
    (if-let [trie (get-trie this ks)]
      (.-val ^Trie trie)
      not-found))

  ;; ----------------------------------------------------------------------

  IPersistentCollection
  (cons [this entry]
    (throw (ex-info "not yet implemented -- 3" {}))
    #_(assoc this (first entry) (second entry)))

  (empty [this] (trie))

  (equiv [this other]
    (and (= (.-key this) (.-key ^Trie other))
         (= (.-val this) (.-val ^Trie other))
         (= (.-children this) (.-children ^Trie other)))))

(defmethod print-method Trie
  [^Trie trie ^Writer w]
  (print-method {:key (.-key trie), :value (.-val trie), :children (.-children trie)} w))

(defn trie
  ([]
   (->Trie nil nil {}))

  ([& kvs]
   (assert (even? (count kvs)) "must provide an even number of elements")
   (reduce
    (fn [accm [k v]] (assoc accm k v))
    (trie)
    (partition 2 kvs))))

(defn get-trie
  [^Trie trie ks]
  (loop [ks ks, trie trie]
    (prn ks trie)
    (cond
      (nil? trie)
      nil

      (empty? ks)
      (->Trie (.-key trie) (.-val trie) (.-children trie))

      :else
      (recur (rest ks) (get (.-children trie) (first ks))))))


(comment

  (def t (trie))
  (get-trie t [\A \B \C])

  (def t2 (->Trie nil nil {\A (->Trie \A nil {\B (->Trie \B nil {\C (->Trie \C 1 {})})})}))
  (get-trie t2 [\A \B \C])


  (assoc (trie) '(\a) 1)
  (trie nil 1)

  (trie "abc" 1 "def" 2)
  (hash-map 1 2 3)



)

(def t2 (->Trie nil nil {\A (->Trie \A nil {\B (->Trie \B nil {\C (->Trie \C 1 {})})})}))
