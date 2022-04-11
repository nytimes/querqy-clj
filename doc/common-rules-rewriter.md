# Common Rules Rewriter

- Overview
- Matching / Rules
- Building a Vocabulary for your domain
- New syntax / unknown edge cases / unclear how far you can push templates

## Matching

- String / Boolean Input

## Transformations

## delete

## synonym

## boost

## filter

## Building a Vocabulary

### Simple

```clojure
(require '[com.nytimes.querqy :as querqy])
(require '[com.nytimes.querqy.replace :as rep])
(require '[com.nytimes.querqy.rules :as rule])

(defn synonyms
  [& strings]
  (let [strings (set strings)]
    (for [string strings]
      (let [syns (map rule/synonym (disj strings string))]
        (rule/match* string syns)))))


(rule/rules-rewriter
  (synonyms "chickpea" "chickpeas" "garbanzo bean" "garbanzo beans"))
```

### Domain Specific

```clojure
(require '[com.nytimes.querqy :as querqy])
(require '[com.nytimes.querqy.replace :as rep])
(require '[com.nytimes.querqy.rules :as rule])

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

