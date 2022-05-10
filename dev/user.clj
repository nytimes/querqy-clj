(ns user
  (:require [com.nytimes.querqy.commonrules :as c]
            [com.nytimes.querqy :as q]
            [clojure.math :as math]))


(defn pin
  "Pin the given IDs to the top of the result set in the order given. Should only
  be used once within a given match rule."
  [& ids]
  (map-indexed
   (fn [idx id]
     (c/boost (- Float/MAX_VALUE idx) {:ids {:values [id]}}))
   ids))


;; Let's assume we have some special thanksgiving related content that editorial
;; wants highly promoted, the documents with IDs 12345 and 5678. Rather than
;; tweak scores or sprinkle boosts around our rule set, we can instead use our
;; new pin rule which makes clear the intent of the boost and serves as
;; documentation for what we're trying to achieve with this rule.
(def rules
  (c/rules-rewriter
   (c/match "thanksgiving"
     (pin "12345" "5678"))))


;;  We can now emit a query which pins results to the top.

(def opts {:match/fields ["headline"]})

(q/emit (q/rewrite rules "thanksgiving recipes") opts)

{:function_score
 {:query
  {:bool
   {:must [],
    :should
    [{:match {"headline" {:query "thanksgiving"}}}
     {:match {"headline" {:query "recipes"}}}],
    :must_not [],
    :filter []}},
  :functions
  [{:filter {:ids {:values ["5678"]}}, :weight 1.0E37}
   {:filter {:ids {:values ["12345"]}}, :weight 1.0E38}]}}
