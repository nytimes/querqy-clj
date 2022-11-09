(ns com.nytimes.querqy.zip
  (:require
   [clojure.zip :as cz]
   [com.nytimes.querqy.node.protocols :as node]))

(defn zipper
  [root]
  #_(assert (instance? node/Node root))
  (cz/zipper node/inner?
             (comp seq node/children)
             node/replace-children
             root))

;; ----------------------------------------------------------------------
;; Movement


;; (defn left [zloc])

;; (defn right [zloc])

;; (defn up [zloc])

;; (defn down [zloc])

;; (defn prev [zloc])

;; (defn next2 [zloc])

;; (defn leftmost [zloc])

;; (defn rightmost [zloc])

;; ----------------------------------------------------------------------

;;(defn find [])



;; (defn delete [])



(comment

  (require '[com.nytimes.querqy.node.term :refer [term-node]])
  (require '[com.nytimes.querqy.node.dismax :refer [dismax-node]])

  (def d1
    (dismax-node [(term-node nil "a" nil)
                  (term-node nil "b" nil)
                  (term-node nil "c" nil)]))

  (def z (zipper d1))



)
