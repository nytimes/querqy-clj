(ns com.nytimes.querqy.elasticsearch
  "Convert a `querqy.model.ExpandedQuery` into an Elasticsearch query.
    You may control the query generation with the following keys in the opts map.
    See the Elasticsearch documentation for their meaning.
    The returned query will always be a function score query whose `:query` object
    is a boolean query. The shape is:
    ```
    {:function_score
      {:query     {:bool
                    {:should   [,,,]
                     :must     [,,,]
                     :must_not [,,,]
                     :filter   []}}
       :functions [,,,]}}
    ```
    Since this query will always be returned in this shape, you can inject
    constant filter or boosting functions into the generated queries for your
    domain.

    The options map accepts the following values. Consult the Elasticsearch
    documentation for their meaning.

    - `:match/fields`
    - `:match/anayzer`
    - `:match/auto_generate_synonyms_phrase_query`
    - `:match/fuzziness`
    - `:match/max_expansions`
    - `:match/prefix_length`
    - `:match/fuzzy_transpositions`
    - `:match/fuzzy_rewrite`
    - `:match/lenient`
    - `:match/operator`
    - `:match/minimum_should_match`
    - `:match/zero_terms_query`
    - `:dis_max/tie_breaker`"
  (:require
   [com.nytimes.querqy.model :as m]
   [com.nytimes.querqy.protocols :as p])
  (:import
   (com.nytimes.querqy.model RawQuery)
   (querqy.model BooleanQuery BoostQuery BoostedTerm DisjunctionMaxQuery ExpandedQuery MatchAllQuery Term)))

(defprotocol InternalEmitter
  (emit* [this opts]))

(defmacro forv [& args] `(vec (for ~@args)))
(def flattenv (comp vec flatten))

(defn- select-match-opts
  [opts]
  (update-keys (select-keys
                opts
                [:match/anayzer
                 :match/auto_generate_synonyms_phrase_query
                 :match/fuzziness
                 :match/max_expansions
                 :match/prefix_length
                 :match/fuzzy_transpositions
                 :match/fuzzy_rewrite
                 :match/lenient
                 :match/operator
                 :match/minimum_should_match
                 :match/zero_terms_query])
               (comp keyword name)))

(extend-protocol InternalEmitter
  Term
  (emit* [term opts]
    (let [fields     (if-let [field (.getField term)]
                       (vector field)
                       (:match/fields opts))
          match-opts (select-match-opts opts)
          query      (str (.getValue term))]
      (when-not (seq fields)
        (throw (IllegalArgumentException.
                (print-str "No fields found for term."
                           "The term must either contain"
                           "a field value or you must set `:match/fields`"))))
      (forv [field fields] {:match {field (merge match-opts {:query query})}})))

  BoostedTerm
  (emit* [term opts]
    (let [fields     (if-let [field (.getField term)]
                       (vector field)
                       (:match/fields opts))
          match-opts (select-match-opts opts)
          query      (str (.getValue term))
          boost      (.getBoost term)]
      (when-not (seq fields)
        (throw (IllegalArgumentException.
                (print-str "No fields found for term."
                           "The term must either contain"
                           "a field value or you must set `:match/fields`"))))
      (forv [field fields] {:match {field (merge match-opts {:boost boost, :query query})}})))

  MatchAllQuery
  (emit* [_ _] {:match_all {}})

  DisjunctionMaxQuery
  (emit* [query {:keys [dis_max/tie_breaker] :as opts}]
    (let [opts    (assoc opts ::parent ::dismax)
          clauses (flattenv
                   (forv [clause (.getClauses query)]
                         (let [query (emit* clause opts)]
                           (if (sequential? query)
                             query
                             (vector query)))))]
      (case (count clauses)
        ;; 0 clauses match nothing
        0 {:match_none {}}
        ;; 1 clause can be promoted
        1 (first clauses)
        ;; build dis_max query
        (cond-> {:dis_max {:queries clauses}}
          tie_breaker (assoc-in [:dis_max :tie_breaker] tie_breaker)))))

  BooleanQuery
  (emit* [query {:keys [::parent] :as opts}]
    (let [get-occur   (comp m/occur->kw m/get-occur)
          clauses     (forv [clause (.getClauses query)]
                            (let [occur  (get-occur clause)
                                  clause (emit* clause opts)]
                              (hash-map occur (if (sequential? clause)
                                                clause
                                                (vector clause)))))
          first-occur (keys (first clauses))
          promotable  #{:should :must}]
      (cond
        ;; 0 clauses matches nothing
        (= 0 (count clauses))
        {:match_none {}}

        ;; 1 should or must clauses can be promoted to the top level
        (and (= 1 (count clauses)) (some promotable first-occur))
        (-> clauses first vals ffirst)

        ;; build a bool query
        :else
        (cond-> {:bool (apply merge-with into clauses)}
          ;; normalize the term weights
          (and (= parent ::dismax) (seq clauses))
          (assoc-in [:bool :boost] (double (/ 1 (count clauses))))))))

  RawQuery
  (emit* [_opts query]
    (:query query))

  BoostQuery
  (emit* [query opts]
    ;; Create a function_score wight query
    {:filter (emit* (.getQuery query) opts)
     :weight (.getBoost query)})

  ExpandedQuery
  (emit* [query opts]
    (let [default-bool  {:bool {:must [], :should [], :must_not [], :filter []}}
          user-query    (emit* (.getUserQuery query) opts)
          user-query    (if (some #{:bool} (keys user-query))
                          (merge-with merge default-bool user-query)
                          (update-in default-bool [:bool :should] conj user-query))
          filters       (forv [query (.getFilterQueries query)]
                              (emit* query opts))
          boost-up      (forv [query (.getBoostUpQueries query)]
                              (emit* query opts))
          boost-down    (forv [^BoostQuery query (.getBoostDownQueries query)]
                              ;; down boost are converted to negative numbers here
                              (let [boosted (m/boostq {:boost (- (.getBoost query))
                                                       :query (.getQuery query)})]
                                (emit* boosted opts)))
          functions     (concat boost-up boost-down)
          default-query {:function_score {:query user-query, :functions []}}]
      (-> default-query
          (update-in [:function_score :query :bool :filter] into filters)
          (update-in [:function_score :functions] into functions)))))

;; ----------------------------------------------------------------------
;;

(deftype ElasticsearchQueryEmitter []
  p/Emitter
  (emit [_ query opts]
    (emit* query opts)))

(defn elasticsearch-query-emitter []
  (ElasticsearchQueryEmitter.))

;; ----------------------------------------------------------------------
;; Helpers

(defn add-must
  "Add a must clause onto the inner boolean query."
  [q & clauses]
  (update-in q [:function_score :query :bool :must] into clauses))

(defn add-should
  "Add a should clause onto the inner boolean query."
  [q & clauses]
  (update-in q [:function_score :query :bool :should] into clauses))

(defn add-must-not
  "Add a must_not clause onto the inner boolean query."
  [q & clauses]
  (update-in q [:function_score :query :bool :must_not] into clauses))

(defn add-filter
  "Add a filter clause onto the inner boolean query."
  [q & clauses]
  (update-in q [:function_score :query :bool :filter] into clauses))

(defn set-minimum-should-match
  "Set the minimum_should_match value for the inner boolean query."
  [q min-match]
  (assoc-in q [:function_score :query :bool :minimum_should_match] min-match))

(defn add-boost
  "Add a boosting function to the outre function score query."
  [q & clauses]
  (update-in q [:function_score :functions] into clauses))
