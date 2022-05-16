(ns com.nytimes.querqy-test
  (:require [clojure.test :refer [deftest]]
            [com.nytimes.querqy :as q]
            [com.nytimes.querqy.commonrules :as c]
            [com.nytimes.querqy.replace :as r]
            [testit.core :refer [facts fact =>]]))

(def typos-rewriter
  (r/replace-rewriter
    (r/replace "ombiles" (r/with "mobile"))
    (r/replace "ihpone" (r/with "iphone"))
    (r/delete "cheap")))

(def rules-rewriter
  (c/rules-rewriter
    (c/match (and "iphone" (not "case"))
               (c/boost 100 {:term {:category "mobiles"}}))))

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

;;

(def test-rules
  (c/rules-rewriter
   (c/match "a"
     (c/synonym "x"))

   (c/match "abc"
     (c/synonym "def"))

   (c/match "b c"
     (c/synonym "y"))

   (c/match "b c d"
     (c/synonym "z x"))

   (c/match "b e"
     (c/synonym "m"))

   (c/match "bb cc dd"
     (c/synonym "z x"))

   (c/match "f"
     (c/synonym "k l"))

   (c/match "g h"
     (c/synonym "n o"))

   (c/match "j"
     (c/synonym "s t")
     (c/synonym "q"))))

(defn str->query
  [s]
  (q/emit (q/rewrite test-rules s) {:match/fields ["f1"]}))

(deftest rewriter-test
  (fact "nil"
    (str->query nil)
    => {:function_score
        {:query
         {:bool {:must     [],
                 :should   [{:match_all {}}],
                 :must_not [],
                 :filter   []}},
         :functions []}})

  (fact "<empty>"
    (str->query "")
    => {:function_score
        {:query
         {:bool {:must     [],
                 :should   [{:match_all {}}],
                 :must_not [],
                 :filter   []}},
         :functions []}})

   (fact "(f1:a | f1:x)"
    (str->query "a")
    => {:function_score
        {:query
         {:bool
          {:must     [],
           :should   [{:dis_max
                       {:queries
                        [{:match {"f1" {:query "a"}}}
                         {:match {"f1" {:query "x"}}}]}}],
           :must_not [],
           :filter   []}},
         :functions []}})

  (fact "(f1:j | f1:q | (+f1:s +f1:t)^0.5)"
      (str->query "j")
      => {:function_score
          {:query     {:bool
                       {:must     [],
                        :should   [{:dis_max
                                    {:queries
                                     [{:match {"f1" {:query "j"}}}
                                      {:match {"f1" {:query "q"}}}
                                      {:bool {:boost 0.5,
                                              :must  [{:match {"f1" {:query "s"}}}
                                                      {:match {"f1" {:query "t"}}}]}}]}}],
                        :must_not [],
                        :filter   []}},
           :functions []}})

  (fact "(f1:a | f1:x) f1:b"
    (str->query "a b")
    => {:function_score
        {:query
         {:bool
          {:must     [],
           :should   [{:dis_max
                       {:queries
                        [{:match {"f1" {:query "a"}}}
                         {:match {"f1" {:query "x"}}}]}}
                      {:match {"f1" {:query "b"}}}],
           :must_not [],
           :filter   []}},
         :functions []}})

  (fact "(f1:abc | f1:def)"
    (str->query "abc")
    => {:function_score
        {:query
         {:bool
          {:must     [],
           :should   [{:dis_max
                       {:queries
                        [{:match {"f1" {:query "abc"}}}
                         {:match {"f1" {:query "def"}}}]}}],
           :must_not [],
           :filter   []}},
         :functions []}})

  (fact "(f1:b | f1:y) (f1:c | f1:y)"
    (str->query "b c")
    => {:function_score
        {:query
         {:bool
          {:must     [],
           :should   [{:dis_max
                       {:queries
                        [{:match {"f1" {:query "b"}}}
                         {:match {"f1" {:query "y"}}}]}}
                      {:dis_max
                       {:queries
                        [{:match {"f1" {:query "c"}}}
                         {:match {"f1" {:query "y"}}}]}}],
           :must_not [],
           :filter   []}},
         :functions []}})

  (fact "(f1:a | f1:x) (f1:b | f1:y) (f1:c | f1:y)"
    (str->query "a b c")
    => {:function_score
        {:query
         {:bool
          {:must     [],
           :should   [{:dis_max
                       {:queries
                        [{:match {"f1" {:query "a"}}}
                         {:match {"f1" {:query "x"}}}]}}
                      {:dis_max
                       {:queries
                        [{:match {"f1" {:query "b"}}}
                         {:match {"f1" {:query "y"}}}]}}
                      {:dis_max
                       {:queries
                        [{:match {"f1" {:query "c"}}}
                         {:match {"f1" {:query "y"}}}]}}],
           :must_not [],
           :filter   []}},
         :functions []}})

  (fact "(f1:g | (+f1:n +f1:o)^0.5) (f1:h | (+f1:n +f1:o)^0.5)"
    (str->query "g h")
    => {:function_score
        {:query
         {:bool
          {:must     [],
           :should   [{:dis_max
                       {:queries
                        [{:match {"f1" {:query "g"}}}
                         {:bool {:boost 0.5,
                                 :must  [{:match {"f1" {:query "n"}}}
                                         {:match {"f1" {:query "o"}}}]}}]}}
                      {:dis_max
                       {:queries
                        [{:match {"f1" {:query "h"}}}
                         {:bool {:boost 0.5,
                                 :must  [{:match {"f1" {:query "n"}}}
                                         {:match {"f1" {:query "o"}}}]}}]}}],
           :must_not [],
           :filter   []}},
         :functions []}}))
