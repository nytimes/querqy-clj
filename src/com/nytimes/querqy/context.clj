(ns com.nytimes.querqy.context
  (:import
   (java.util Optional)
   (querqy.rewrite SearchEngineRequestAdapter)))

(defn optional
  ([] (Optional/empty))
  ([x] (Optional/ofNullable x)))

(defrecord Context [chain debug? params context info-logging-context]
  SearchEngineRequestAdapter
  (getRewriteChain [_] chain)
  (getContext [_] context)
  (getRequestParam [_ k] (optional (get params k)))
  (getRequestParams [_ k] (into-array String (mapv str (get params k))))
  (getBooleanRequestParam [_ k] (some-> (get params k) boolean optional))
  (getIntegerRequestParam [_ k] (some-> (get params k) int optional))
  (getFloatRequestParam [_ k] (some-> (get params k) float optional))
  (getDoubleRequestParam [_ k] (some-> (get params k) double optional))
  (isDebugQuery [_] debug?)
  (getInfoLoggingContext [_] (optional)))

(def empty-context
  (map->Context
   {:chain   []
    :debug?  false
    :params  {}
    :context {}}))

