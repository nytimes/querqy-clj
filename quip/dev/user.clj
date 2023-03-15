(ns user
  (:require
   [nytimes.quip.node :as node]
   [nytimes.quip.zip :as z]
   [clojure.string :as str]))

(defn parse
  [string]
  (node/query
   (node/subquery
    :query (node/bool
            (for [token (str/split string #"\s+")]
              (node/clause :should (node/dismax [(node/term token)])))))))

(comment

  (def root (z/zipper (parse "hello world")))

  (-> root z/next z/next z/next z/node)
  (-> root z/down z/down z/right z/node)



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
