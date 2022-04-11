# Intro

An example is helpful to illustrate what you can do with querqy.

```clojure
(require '[com.nytimes.querqy :as querqy])
(require '[com.nytimes.querqy.replace :as rep])
(require '[com.nytimes.querqy.rules :as rule])


(def typos-rewriter
  (rep/replace-rewriter
    (rep/replace "ombiles" (rep/with "mobile"))
    (rep/replace "ihpone" (rep/with "iphone"))
    (rep/delete "cheap")))

(def rules-rewriter
  (rule/rules-rewriter
    (rule/match (and "iphone" (not "case"))
      (rule/boost 100 {:term {:category "mobiles"}}))))

(def chain
  (querqy/chain-of [typos-rewriter rules-rewriter]))

;; rewriter chain will delete cheap, correct ihpone typo, and boost mobile phone category

(def emit-opts
  {:match/fields        ["title", "body"]
   :dis_max/tie_breaker 0.5})


(querqy/emit (querqy/rewrite chain "cheap ihpone")
             emit-opts)
;; =>
{:function_score
 {:query
  {:bool
   {:must     [],
    :should   [{:dis_max
                {:queries     [{:match {"title" {:query "iphone"}}}
                               {:match {"body" {:query "iphone"}}}],
                 :tie_breaker 0.5}}],
    :must_not [],
    :filter   []}},
  :functions [{:filter {:term {:category "mobiles"}},
               :weight 100.0}]}}

  ```

