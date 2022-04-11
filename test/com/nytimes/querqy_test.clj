(ns com.nytimes.querqy-test
  (:require [clojure.test :refer [deftest is]]
            [com.nytimes.querqy :as q]
            [com.nytimes.querqy.rules :as rul]
            [com.nytimes.querqy.replace :as rep]
            [testit.core :refer [facts =>]]))

(def typos-rewriter
  (rep/replace-rewriter
    (rep/replace "ombiles" (rep/with "mobile"))
    (rep/replace "ihpone" (rep/with "iphone"))
    (rep/delete "cheap")))

(def rules-rewriter
  (rul/rules-rewriter
    (rul/match (and "iphone" (not "case"))
               (rul/boost 100 {:term {:category "mobiles"}}))))

(def chain (q/chain-of [typos-rewriter rules-rewriter]))

;; rewriter chain will delete cheap, correct ihpone typo, and boost mobile phone category

(deftest full-test
  (let [rewrite (fn [string]
                  (q/emit (q/rewrite chain string)
                          {:match/fields        ["title", "body"]
                           :dis_max/tie_breaker 0.5}))]
    (facts "rewrite tests"
      (rewrite "cheap ihpone")
      => {:function_score
          {:query     {:bool {:must     [],
                              :should
                              [{:dis_max
                                {:queries
                                 [{:match {"title" {:query "iphone"}}}
                                  {:match {"body" {:query "iphone"}}}],
                                 :tie_breaker 0.5}}],
                              :must_not [],
                              :filter   []}},
           :functions [{:filter {:term {:category "mobiles"}}, :weight 100.0}]}})))


