(ns com.nytimes.querqy.zip
  (:require
   [clojure.zip :as cz]
   [com.nytimes.querqy.node.protocols :as node]))

(defn query-zipper
  "Create a zipper to work with Queries."
  [root]
  #_(assert (instance? node/Node root))
  (cz/zipper node/inner?
             (comp seq node/children)
             node/replace-children
             root))






(comment

  (require '[com.nytimes.querqy.node.term :refer [term-node]])
  (require '[com.nytimes.querqy.node.dismax :refer [dismax-node]])
  (require '[com.nytimes.querqy.node.boolean :refer [boolean-node clause-node should]])
  (require '[com.nytimes.querqy.node.boost :refer [boost-node]])

  (def d1
    (dismax-node [(term-node nil "a" nil)
                  (term-node nil "b" nil)
                  (term-node nil "c" nil)]))

  (def d2
    (-> (boolean-node)
        (should (dismax-node
                 [(term-node nil "a" nil)
                  (term-node nil "b" nil)
                  (term-node nil "c" nil)]))))

  (def z (query-zipper d1))
  (def zseq (iterate cz/next z))

  (defn iter-zip [zipper]
  (->> zipper
       (iterate cz/next)
       (take-while (complement cz/end?))))



)
