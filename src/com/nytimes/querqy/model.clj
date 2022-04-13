(ns com.nytimes.querqy.model
  "Builders for classes in the `querqy.model` package."
  (:require
    [clojure.core.protocols :as cp]
    [clojure.datafy :refer [datafy]]
    [clojure.string :as str]
    [clojure.spec.alpha :as s])
  (:import
    (querqy.model
      BooleanParent BooleanQuery BoostedTerm BoostQuery
      Clause Clause$Occur DisjunctionMaxQuery
      ExpandedQuery Input$SimpleInput MatchAllQuery
      QuerqyQuery Query Term)))

(def should Clause$Occur/SHOULD)
(def must Clause$Occur/MUST)
(def must-not Clause$Occur/MUST_NOT)

(defn get-occur [^Clause clause]
  (.getOccur clause))

(defn occur->kw [^Clause$Occur occur]
  (keyword (str/lower-case (.name occur))))

(defn term? [obj] (instance? Term obj))

(defn term
  [{:keys [parent field value generated]
    :or   {generated false}}]
  {:pre [(string? value)]}
  (Term. parent field value generated))

(defn boosted-term? [obj] (instance? BoostedTerm obj))

(defn boosted-term
  [{:keys [parent field value boost]}]
  {:pre [(string? value) (number? boost)]}
  (BoostedTerm. parent field value boost))

(defn match-all? [obj] (instance? MatchAllQuery obj))

(defn match-all [] (MatchAllQuery.))

(defn dismaxq? [obj] (instance? DisjunctionMaxQuery obj))

(defn dismaxq
  [{:keys [parent occur generated clauses]
    :or   {occur     should
           generated false}}]
  (let [query (DisjunctionMaxQuery. parent occur generated)]
    (doseq [^Clause clause clauses]
      (.addClause query (.clone clause query)))
    query))

(defn boostq? [obj] (instance? BoostQuery obj))

(defn boostq
  [{:keys [^QuerqyQuery query boost]}]
  {:pre [(number? boost)]}
  (BoostQuery. query boost))

(defn boolq? [obj] (instance? BooleanQuery obj))

(defn boolq
  [{:keys [^BooleanParent parent
           ^Clause$Occur occur
           generated
           clauses]
    :or   {generated false
           occur     should
           clauses   []}}]
  (let [bq (BooleanQuery. parent occur generated)]
    (doseq [^Clause clause clauses]
      (.addClause bq (.clone clause bq)))
    bq))

(defrecord RawQuery [parent occur query generated?]
  QuerqyQuery
  (clone [this new-parent]
    (RawQuery. new-parent occur query generated?))
  (clone [this new-parent generated?]
    (RawQuery. new-parent occur query generated?)))

(defn rawq? [obj] (instance? RawQuery obj))

(defn rawq
  [{:keys [parent occur query generated]
    :or   {occur     should
           generated false}}]
  (RawQuery. parent occur query generated))

(defn q? [obj] (instance? Query obj))

(defn q
  [{:keys [generated clauses]
    :or   {generated false}}]
  (let [query (Query. generated)]
    (doseq [^Clause clause clauses]
      (.addClause query (.clone clause query)))
    query))

(defn querqyq? [obj] (instance? QuerqyQuery obj))

(defn expandedq? [obj] (instance? ExpandedQuery obj))

(defn expandedq
  [{:keys [query boost-up boost-down filter]
    :or   {boost-up   []
           boost-down []
           filter     []}}]
  (ExpandedQuery. query filter boost-up boost-down))

;; ----------------------------------------------------------------------
;;  datafy

(extend-protocol cp/Datafiable
  BooleanQuery
  (datafy [^BooleanQuery q]
    {:type    BooleanQuery
     :occur   (.getOccur q)
     :clauses (mapv datafy (.getClauses q))})

  BoostQuery
  (datafy [^BoostQuery q]
    {:type  BoostQuery
     :boost (.getBoost q)
     :query (datafy (.getQuery q))})

  RawQuery
  (datafy [^RawQuery q]
    {:type  RawQuery
     :query (.-query q)})

  DisjunctionMaxQuery
  (datafy [^DisjunctionMaxQuery q]
    {:type    DisjunctionMaxQuery
     :occur   (occur->kw (.getOccur q))
     :clauses (mapv datafy (.getClauses q))})

  ExpandedQuery
  (datafy [^ExpandedQuery q]
    {:type       ExpandedQuery
     :user-query (datafy (.getUserQuery q))
     :boost-up   (mapv datafy (.getBoostUpQueries q))
     :boost-down (mapv datafy (.getBoostDownQueries q))
     :filter     (mapv datafy (.getFilterQueries q))})

  Input$SimpleInput
  (datafy [^Input$SimpleInput i]
    {:type            Input$SimpleInput
     :terms           (mapv datafy (.getInputTerms i))
     :left-boundary?  (.isRequiresLeftBoundary i)
     :right-boundary? (.isRequiresRightBoundary i)})

  MatchAllQuery
  (datafy [^MatchAllQuery _q]
    {:type      MatchAllQuery
     :match_all {}})

  Query
  (datafy [^Query q]
    {:type    Query
     :occur   (occur->kw (.getOccur q))
     :clauses (mapv datafy (.getClauses q))})

  BoostedTerm
  (datafy [^BoostedTerm t]
    {:type  BoostedTerm
     :field (.getField t)
     :boost (.getBoost t)
     :value (str (.getValue t))})

  Term
  (datafy [^Term t]
    {:type  Term
     :field (.getField t)
     :value (str (.getValue t))}))
