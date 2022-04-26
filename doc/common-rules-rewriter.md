# Common Rules Rewriter

The common rules rewriter is useful to a transform user's query into something
which more closely matches the documents indexed.

## Usage

### Querqy File

You can read in a text file written using Querqy's [CommonRulesRewriter][1] syntax.

```clojure
(require '[com.nytimes.querqy.commonrules :refer [rules-rewriter]])
(require '[clojure.java.io :as io])

(rules-rewriter (io/resource "common-rules.txt"))
```

### DSL

The `querqy-clj` library ships with a DSL for creating a common rules rewriter from Clojure.

```clojure
(require '[com.nytimes.querqy.commonrules :as c])

(def rewriter
  (c/rules-rewriter
   (c/match "apple phone"
     (c/synonym "iphone"))

   (c/match "nikon d500"
     (c/filter {:term {:category "cameras"}}))))
```

## Building a Language

You can use the transform functions to build your own custom set of transform
which make sense for your domain. This can simplify complex transforms and also
create a vocabulary for the intent of a given transform.

The next two sub-sections contain examples.

### Simple

When multiple terms all refer to the same entity, it can get tedious to write
out all of the forms:

```clojure
(c/match
 "x" 
  (c/synonym "y")
  (c/synonym "z"))
(c/match "y" 
  (c/synonym "x")
  (c/synonym "z"))
(c/match "z" 
  (c/synonym "x")
  (c/synonym "y"))
```

Instead, it would be great to just write this information in a single rule.
Let's assume we have a domain where we have many different terms for the same
entity. We will use recipes here, since there are many different terms for food.

We can capture this idea by creating a simple synonyms rule.

```clojure
(require '[com.nytimes.querqy :as querqy])
(require '[com.nytimes.querqy.rules :as c])

(defn synonyms
  [& strings]
  (let [strings (set strings)]
    (for [string strings]
      (let [syns (map rule/synonym (disj strings string))]
        (c/match* string syns)))))


(c/rules-rewriter
  (synonyms "bread" "toast")
  (synonyms "chickpea" "garbanzo bean"))
```

### Domain Specific

```clojure
(require '[com.nytimes.querqy :as querqy])
(require '[com.nytimes.querqy.commonrules :as c])

(defn boost-movies [& {:keys [with by]}]
  (rule/boost by {:bool {:must [{:term {:type "movie"}}
                                {:term {:actors with}}]}}))

(defn boost-series [& {:keys [with by]}]
  (rule/boost by {:bool {:must [{:term {:type "series"}}
                                {:term {:actors with}}]}}))

(defn highlight-movie [movie]
  (rule/boost 10000 {:match {:title movie}}))

(def tmdb-rules
  (rule/rules-rewriter
    (rule/match "christian bale"
                (boost-movies :with "Christian Bale" :by 3)
                (boost-series :with "Christian Bale" :by 1.5))

    (rule/match (and "christian bale" "batman")
                (highlight-movie "Batman Begins"))))


(def emit-opts
  {:match/fields        ["title", "body"]
   :dis_max/tie_breaker 0.5})


(querqy/emit (querqy/rewrite tmdb-rules "christian bale") emit-opts)
;; =>
{:function_score
 {:query
  {:bool {:must     [],
          :should   [{:dis_max {:queries     [{:match {"title" {:query "christian"}}}
                                              {:match {"body" {:query "christian"}}}],
                                :tie_breaker 0.5}}
                     {:dis_max {:queries     [{:match {"title" {:query "bale"}}}
                                              {:match {"body" {:query "bale"}}}],
                                :tie_breaker 0.5}}],
          :must_not [],
          :filter   []}},
  :functions
  [{:filter {:bool {:must [{:term {:type "series"}} {:term {:actors "Christian Bale"}}]}},
    :weight 1.5}
   {:filter {:bool {:must [{:term {:type "movie"}} {:term {:actors "Christian Bale"}}]}},
    :weight 3.0}]}}



(querqy/emit (querqy/rewrite tmdb-rules "batman movie christian bale")
             emit-opts)
;; =>
{:function_score
 {:query
  {:bool {:must     [],
          :should   [{:dis_max {:queries     [{:match {"title" {:query "batman"}}}
                                              {:match {"body" {:query "batman"}}}],
                                :tie_breaker 0.5}}
                     {:dis_max {:queries     [{:match {"title" {:query "movie"}}}
                                              {:match {"body" {:query "movie"}}}],
                                :tie_breaker 0.5}}
                     {:dis_max {:queries     [{:match {"title" {:query "christian"}}}
                                              {:match {"body" {:query "christian"}}}],
                                :tie_breaker 0.5}}
                     {:dis_max {:queries     [{:match {"title" {:query "bale"}}}
                                              {:match {"body" {:query "bale"}}}],
                                :tie_breaker 0.5}}],
          :must_not [],
          :filter   []}},

  :functions
  [{:filter {:bool {:must [{:term {:type "series"}} {:term {:actors "Christian Bale"}}]}},
    :weight 1.5}
   {:filter {:bool {:must [{:term {:type "movie"}} {:term {:actors "Christian Bale"}}]}},
    :weight 3.0}
   {:filter {:match {:title "Batman Begins"}},
    :weight 10000.0}]}}
```

[1]: https://querqy.org/docs/querqy/rewriters/common-rules.html#structure-of-a-rule
