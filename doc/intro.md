# Intro

An example is helpful to illustrate what you can do with Querqy. 

## Imports

These are the three most common name spaces you will need when creating your
rewriter.

```clojure
;; The core querqy namespace
(require '[com.nytimes.querqy :as querqy])

;; The replace rewriter
(require '[com.nytimes.querqy.replace :as rep])

;; The common rules rewriter
(require '[com.nytimes.querqy.commonrules :as rul])
```

## Replace Rewriter

A common use case for the replace rewriter is to fix typos, consolidate input
into a common form, and delete unhelpful query terms. In the below example we do all three:

1. We fix the typo `ihpone` and replace it with `iphone`.
2. We fix `ombiles` and replace it with `mobile`. We also replace the plural
   `mobiles` with the singular `mobile` This would also be a good place to
   capture `cell phone`, `cell phones`, etc.
3. We delete `cheap` from the input because, in this hypothetical, it hurts search relevance.

```clojure
(def typos-rewriter
  (rep/replace-rewriter

    (rep/replace "ihpone" (rep/with "iphone"))

    (rep/replace (or "mobiles" "ombiles")
      (rep/with "mobile"))

    (rep/delete "cheap")))
```

## Common Rules Rewriter

The common rules rewriter is where we will write most of the domain-specific
query rewriting we want to do. In our example, we have three rules:

1. When we see a query with `apple phone`, we inject a synonym `iphone` into the
   query.
2. When a query contains `iphone` but not `case`, we boost all matching documents
   tagged with the `iphone` category.
3. When a query contains `iphone` and `case`, we boost all matching documents
   tagged with the `iphoe-accessories` category.

```clojure


(def rules-rewriter
  (rul/rules-rewriter

   (rul/match "apple phone"
     (rul/synonym "iphone"))

   (rul/match (and "iphone" (not "case"))
      (rul/boost 100 {:term {:category "iphone"}}))

   (rul/match (and "iphone" "case")
      (rul/boost 100 {:term {:category "iphone-accessories"}}))))
```

## Rewriter Chain

We can compose multiple rewriters into a single rewriter chain. The rewriters
run in order, so this chain will cause our typo rewriter to run before our rules
rewriter.

```clojure
(def chain (querqy/chain-of [typos-rewriter rules-rewriter]))
```

## Emitting an Elasticsearch Query

Below we use our rewriter chain to rewrite the query `cheap ihpone`. We emit an
Elasticsearch query from this which we can issue against our search cluster.

```clojure
(def emit-opts
  {:match/fields        ["title", "body"]
   :dis_max/tie_breaker 0.5})


(def query "cheap ihpone")


(def query' (querqy/rewrite chain query))

(querqy/emit query' emit-opts)
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


<!-- LocalWords: Querqy rewriter rewriters -->
