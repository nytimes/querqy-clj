(ns com.nytimes.querqy.model
  "Builders for classes in the `querqy.model` package."
  (:require
   [clojure.core.protocols :as cp]
   [clojure.datafy :refer [datafy]]
   [clojure.string :as str])
  (:import
   (querqy.model BooleanParent BooleanQuery BoostQuery BoostedTerm Clause Clause$Occur DisjunctionMaxQuery ExpandedQuery Input$SimpleInput MatchAllQuery QuerqyQuery Query Term)))

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
  (clone [_ new-parent]
    (RawQuery. new-parent occur query generated?))
  (clone [_ new-parent generated?]
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
  (ExpandedQuery. query filter boost-up boost-down []))

;; ----------------------------------------------------------------------
;;  datafy

(extend-protocol cp/Datafiable
  BooleanQuery
  (datafy [^BooleanQuery q]
    (-> (group-by (comp occur->kw get-occur) (.getClauses q))
        (update-keys (fn [k] (keyword "bool" (name k))))
        (update-vals (partial mapv datafy))))

  BoostQuery
  (datafy [^BoostQuery q]
    {:boost/amount (.getBoost q)
     :boost/query  (datafy (.getQuery q))})

  RawQuery
  (datafy [^RawQuery q]
    {:raw/query (.-query q)})

  DisjunctionMaxQuery
  (datafy [^DisjunctionMaxQuery q]
    {:dismax/clauses (mapv datafy (.getClauses q))})

  ExpandedQuery
  (datafy [^ExpandedQuery q]
    (cond-> {:expanded/query (datafy (.getUserQuery q))}
      (.getBoostUpQueries q)
      (assoc :expanded/boost-up (mapv datafy (.getBoostUpQueries q)))

      (.getBoostDownQueries q)
      (assoc :expanded/boost-down (mapv datafy (.getBoostDownQueries q)))

      (.getFilterQueries q)
      (assoc :expanded/filter (mapv datafy (.getFilterQueries q)))))

  Input$SimpleInput
  (datafy [^Input$SimpleInput i]
    {:type            Input$SimpleInput
     :terms           (mapv datafy (.getInputTerms i))
     :left-boundary?  (.isRequiresLeftBoundary i)
     :right-boundary? (.isRequiresRightBoundary i)})

  MatchAllQuery
  (datafy [^MatchAllQuery _q]
    {:match_all {}})

  Query
  (datafy [^Query q]
    (-> (group-by (comp occur->kw get-occur) (.getClauses q))
        (update-keys (fn [k] (keyword "query" (name k))))
        (update-vals (partial mapv datafy))))

  BoostedTerm
  (datafy [^BoostedTerm t]
    {:boost/field  (.getField t)
     :boost/amount (.getBoost t)
     :boost/value  (str (.getValue t))})

  Term
  (datafy [^Term t]
    (cond-> {:term/value (str (.getValue t))}
      (.getField t)
      (assoc :term/field (.getField t)))))
