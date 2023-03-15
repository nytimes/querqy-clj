(ns user
  (:require
   [puget.printer :refer [cprint]]
   [nytimes.quip.node :as node]
   [nytimes.quip.zip :as z]
   [clojure.string :as str]
   [clojure.pprint :refer [pprint]]))

(defn parse
  [string]
  (node/query
   [(node/bool
     (for [token (str/split string #"\s+")]
       (node/should (node/dismax [(node/term token)]))))]))

(defn synonym
  [zloc term syn]
  (-> (z/find-node zloc (partial = term))
      (z/up)
      (z/edit update :terms conj syn)
      (z/root)))

(defn boost
  [zloc term amount]
  (-> (z/find-node zloc (partial = term))
      (z/edit node/boost-term amount)
      (z/root)))

(def root (z/zipper (parse "hello world")))

(comment
  (-> root z/next z/next z/node)
  (-> root z/down z/down z/right z/node)

    (-> root
      (z/find-node node/term?)
      (z/up)
      (z/edit update :terms conj (node/term "hi"))
      (z/root)
      (cprint))

  (-> root
      (synonym (node/term "hello") (node/term "hi"))
      cprint)

  (boost (node/term "hello") 100)
  (-> root
           (boost (node/term "hello") 100)
      :bool
      :clauses
      first)
  (boost root (node/term "hello") 1.0)

  (meta
   (vary-meta {} assoc :x 1))










)
