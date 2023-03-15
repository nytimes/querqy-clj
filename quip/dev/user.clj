(ns user
  (:require
   [nytimes.quip.node :as node]
   [nytimes.quip.zip :as qz]))

(comment

  (def ast (node/dismax [(node/term :text "hello") (node/term :text "world")]))
  (def z (qz/zipper ast))

  (-> z qz/next qz/node)
  (-> z qz/next qz/next qz/next qz/end?)
  (-> z qz/next (qz/edit assoc :text "bye") qz/root)




  (->> z
       (iterate qz/next)
       (take-while identity)
       (take-while (complement qz/end?))
       (drop-while (fn [x] (not (node/term? (qz/node x)))))
       (first)
       (qz/node)
       pr)

  (qz/node (qz/find z (fn [loc] (node/term? (qz/node loc)))))
  (-> (qz/find-node z node/term?)
      (qz/edit assoc :text "goodbye")
      (qz/root)
      )





)
