(ns com.nytimes.querqy.model
  "Builders for classes in the `querqy.model` package."
  (:require
   [clojure.core.protocols :as cp]
   [clojure.datafy :refer [datafy]])
  (:import
   (querqy.model
    BooleanParent
    BooleanQuery
    BoostQuery
    BoostedTerm
    Clause
    Clause$Occur
    DisjunctionMaxQuery
    ExpandedQuery
    Input$SimpleInput
    MatchAllQuery
    QuerqyQuery
    Query
    Term)))

;; ----------------------------------------------------------------------
;; Helpers

(def should Clause$Occur/SHOULD)
(def must Clause$Occur/MUST)
(def must-not Clause$Occur/MUST_NOT)

(defn get-occur [^Clause clause]
  (.getOccur clause))

(def occur->kw
  {should   :should
   must     :must
   must-not :must-not})

(def kw->occur
  {:should   should
   :must     must
   :must-not must-not})

;; ----------------------------------------------------------------------
;; Term

(defn term
  "Create a `querqy.model.Term`."
  ([value]
   (term nil value nil))

  ([field value]
   (term field value nil))

  ([field value {:keys [parent generated] :or {generated false}}]
   (assert (string? value))
   (assert (boolean? generated))
   (Term. parent field value generated)))

(defn term?
  "Return true if object is a `querqy.model.Term`"
  [obj]
  (instance? Term obj))

(extend-protocol cp/Datafiable
  Term
  (datafy [^Term t]
    (with-meta
      (cond-> {:term (str (.getValue t))}
        (.getField t)
        (assoc :field (.getField t)))
      {:type Term})))

;; ----------------------------------------------------------------------
;; BoostedTerm

(defn boosted-term
  "Create a `querqy.model.BoostedTerm`."
  ([value boost]
   (boosted-term nil value boost nil))

  ([field value boost]
   (boosted-term field value boost nil))

  ([field value boost {:keys [parent]}]
   (assert (string? value))
   (assert (number? boost))
   (BoostedTerm. parent field value boost)))

(defn boosted-term?
  [obj]
  (instance? BoostedTerm obj))

(extend-protocol cp/Datafiable
  BoostedTerm
  (datafy [^BoostedTerm t]
    (with-meta
      (cond-> {:term  (str (.getValue t))
               :boost (.getBoost t)}
        (.getField t)
        (assoc :field (.getField t)))
      {:type BoostedTerm})))

;; ----------------------------------------------------------------------
;; MatchAllQuery

(defn match-all? [obj] (instance? MatchAllQuery obj))

(defn match-all [] (MatchAllQuery.))

(extend-protocol cp/Datafiable
  MatchAllQuery
  (datafy [_] {:match_all {}}))

;; ----------------------------------------------------------------------
;; DisjunctionMaxQuery

(defn dismax
  ([clauses]
   (dismax :should clauses nil))

  ([occur clauses]
   (dismax occur clauses nil))

  ([occur clauses {:keys [parent generated] :or {generated false}}]
   (let [occur (kw->occur occur)
         query (DisjunctionMaxQuery. parent occur generated)]
     (doseq [^Clause clause clauses]
       (.addClause query (.clone clause query)))
     query)))

(defn dismax?
  [obj]
  (instance? DisjunctionMaxQuery obj))

(extend-protocol cp/Datafiable
  DisjunctionMaxQuery
  (datafy [^DisjunctionMaxQuery q]
    (set (mapv datafy (.getClauses q)))))

;; ----------------------------------------------------------------------
;; BoostQuery

(defn boost-query
  [boost ^QuerqyQuery query]
  {:pre [(number? boost)]}
  (BoostQuery. query boost))

(defn boost-query?
  [obj]
  (instance? BoostQuery obj))

(extend-protocol cp/Datafiable
  BoostQuery
  (datafy [^BoostQuery q]
    (with-meta
      {:query (datafy (.getQuery q))
       :boost (.getBoost q)}
      {:type BooleanQuery})))

;; ----------------------------------------------------------------------
;; BooleanQuery

(defn bool
  ([clauses]
   (bool :should clauses nil))

  ([occur clauses]
   (bool occur clauses nil))

  ([occur clauses {:keys [^BooleanParent parent generated] :or {generated false}}]
   (let [occur (kw->occur occur)
         query (BooleanQuery. parent occur generated)]
     (doseq [^Clause clause clauses]
       (.addClause query (.clone clause query)))
     query)))

(defn bool?
  [obj]
  (instance? BooleanQuery obj))

(extend-protocol cp/Datafiable
  BooleanQuery
  (datafy [^BooleanQuery q]
    (with-meta
      (-> (group-by (comp occur->kw get-occur) (.getClauses q))
          (update-vals (partial mapv datafy)))
      {:type BooleanQuery})))

;; ----------------------------------------------------------------------
;; 

(defrecord RawQuery [parent occur query generated?]
  QuerqyQuery
  (clone [_ new-parent]
    (RawQuery. new-parent occur query generated?))
  (clone [_ new-parent generated'?]
    (RawQuery. new-parent occur query generated'?)))

(defn rawq? [obj] (instance? RawQuery obj))

(defn rawq
  [{:keys [parent occur query generated]
    :or   {occur     should
           generated false}}]
  (RawQuery. parent occur query generated))

;; ----------------------------------------------------------------------
;; Query

(defn query
  ([clauses]
   (query clauses nil))

  ([clauses {:keys [generated] :or {generated false}}]
   (let [query (Query. generated)]
     (doseq [^Clause clause clauses]
       (.addClause query (.clone clause query)))
     query)))

(defn query?
  [obj]
  (instance? Query obj))

;; ----------------------------------------------------------------------
;; ExpandedQuery

(defn expanded
  [query & {:keys [boost-up boost-down filter]
            :or   {boost-up   []
                   boost-down []
                   filter     []}}]
  (ExpandedQuery. query filter boost-up boost-down))

(defn expanded?
  [obj]
  (instance? ExpandedQuery obj))

(extend-protocol cp/Datafiable
  ExpandedQuery
  (datafy [^ExpandedQuery q]
    (let [boosts (concat (.getBoostUpQueries q) (.getBoostDownQueries q))]
      (with-meta
        (cond-> {:query (datafy (.getUserQuery q))}
          (.getFilterQueries q)
          (assoc :filter (mapv datafy (.getFilterQueries q)))

          (seq boosts)
          (assoc :boost (mapv datafy boosts)))
        {:type ExpandedQuery}))))

;; ----------------------------------------------------------------------
;;  datafy

(extend-protocol cp/Datafiable

  RawQuery
  (datafy [^RawQuery q]
    {:raw/query (.-query q)})

  Input$SimpleInput
  (datafy [^Input$SimpleInput i]
    {:type            Input$SimpleInput
     :terms           (mapv datafy (.getInputTerms i))
     :left-boundary?  (.isRequiresLeftBoundary i)
     :right-boundary? (.isRequiresRightBoundary i)})

  Query
  (datafy [^Query q]
    (-> (group-by (comp occur->kw get-occur) (.getClauses q))
        (update-vals (partial mapv datafy)))))
