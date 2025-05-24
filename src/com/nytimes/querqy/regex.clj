(ns com.nytimes.querqy.regex
  "A regular expression engi
  ne that works on tokens. This namespace exposes
  functions with the same names and behaviors as Clojure core but designed to
  work on a token sequences.

  ## Key Functions

  ### re-matches

  [[re-matches]] will attempt to match the entire input against the regular
  expression.

  ```clojure
  ;; Match
  > (re-matches (alt \"nyc\" (cat \"new\" \"york\" \"city\"))
                (tokenize \"new york city\"))
  [{:text \"new\"} {:text \"york\"} {:text \"city\"}]


  ;; No match
  > (re-matches (alt \"nyc\" (cat \"new\" \"york\" \"city\"))
                (tokenize \"new york\"))
  nil
  ```

  ### re-find

  [[re-find]] will find the first subsequence match, if one exists.

  ```clojure
  > (def month (regex #\"jan|feb|mar|...\"))
  > (def day-of-month (& digit? #(<= 1 (parse-long (:text %)) 31)))
  > (def year (regex #\"\\d{4}\"))
  > (re-find (cat month day-of-month year)
             (tokenize \"feb 6 2002 cookie recipe for chocolate quakes\"))
  [{:text \"feb\"} {:text \"6\"} {:text \"2002\"}]
  ```

  ### re-seq

  [[re-seq]] will find all non-overlapping subsequence matches in the input.

  ```clojure
  > (def country (alt (cat \"united\" \"states\") (regex #\"usa|canada|mexico\")))
  > (re-seq country (tokenize \"united states mexico corn ban\"))
  ([{:text \"united\"} {:text \"states\"}]
   [{:text \"mexico\"}])
  ```

  ## Operators

  ### Value

  These are operators that will match a single token. The simplest is [[token]]
  which accepts a string and will match a token containing exactly that text.
  Additionally there is [[regex]] which accepts a [[java.util.regex.Pattern]]
  and will match tokens matching the pattern. Finally there is [[predicate]]
  which will create a matcher based on any function. This function must accept a
  single token and return a truthy value indicate whether or not the token
  matches.

  All of these value matchers allow for a short hand notation as shown below.

  ```clojure
  (token \"nyc\")      ≡ \"nyc\"
  (regex #\"\\d+\")     ≡ #\"\\d+\"
  (predicate digit?) ≡ digit?
  ```
  ### Concatenation

  Concatenation will match a sequence of tokens. It is created using the `cat`
  operator.

  ```clojure
  (cat \"a\" \"b\")
  ```

  will match the token `a` followed by the token `b`.

  As a shorthand, you may specify concatention using a vector.

  ```clojure
  (cat \"a\" \"b\") ≡ [\"a\" \"b\"]
  ```

  ### Alternation

  Alernation allows for matching multiple possible valid options.

  ```clojure
  ;; Match the token a or b
  > (alt \"a\" \"b\")
  ```

  ### Repetition

  Repetition allows for the specification of multiple matches of a pattern. The
  Gnosis regex engine supports both * and + operators.

  ```clojure
  ;; a followed by any number of b tokens
  > (cat \"a\" (* \"b\"))

  ;; a follow by at least one b token
  > (cat \"a\" (+ \"b\"))
  ```

  ### Refinement

  The refinement operator allows for modifying an existing operator to apply
  more specific requirements.

  ```clojure
  > (defn twentieth-century? [token] (<= 1901 (parse-long (:text token)) 2000))
  ;; Math a token that is made up of digits and whose digits occur within the
  20th century.
  > (& digit? twentieth-century?)
  ```"
  (:refer-clojure :exclude [cat * + & find re-pattern re-matcher re-matches re-find re-seq])
  (:require
   [clojure.core.protocols :refer [Datafiable]]
   [clojure.datafy :refer [datafy]]))

;; ----------------------------------------------------------------------
;; Operators

(defrecord Accept [])

(def accept (->Accept))

(defn accept? [x] (identical? accept x))

(defrecord Token [f])

(defn predicate
  "Match a **token** with any predicate function. This function must accept a single
  argument, the token, and return a truthy value."
  [f]
  (when-not (fn? f)
    (throw (IllegalArgumentException. "predicate requires a function")))
  (->Token f))

(defn token
  "Match a token with with the given string."
  [^String string]
  (when-not (string? string)
    (throw (IllegalArgumentException. "token requires a string")))
  (map->Token
   {:f    (fn [{:keys [^String text]}]
            (when text
              (.equals string text)))
    :text string}))

(defn regex
  "Match a token with with the given regex."
  [pattern]
  (when-not (or (string? pattern) (instance? java.util.regex.Pattern pattern))
    (throw (IllegalArgumentException. "regex requires a string or a java.util.regex.Pattern.")))
  (let [pattern (clojure.core/re-pattern pattern)]
    (map->Token
     {:f       (fn [{:keys [^String text]}]
                 (when text
                   (.matches (.matcher pattern text))))
      :pattern pattern})))

(defrecord Concat [rs])

(defn- one?
  [coll]
  (= 1 (count coll)))

(defn ^:no-doc cat*
  [rs]
  ;; term simplification. order matters. 0 and 1 base cases *must* occur first.
  (when (seq rs)
    (cond
      (empty? rs)          nil
      (one? rs)            (first rs)
      (accept? (first rs)) (cat* (rest rs))
      (some nil? rs)       nil
      :else                (->Concat rs))))

(defn ^:no-doc conscat
  "A helper to build a concatenation from one regex and a list of regexes.

      (conscat r1 [r2 r3 r4])

  is equivalent to

      (cat r1 r2 r3 r4)"
  [r rs]
  (assert (sequential? rs))
  (cat* (cons r rs)))

(defn cat
  "Create a regex matcher that is the concatenation of the given regular
  expressions."
  [& rs]
  (cat* rs))

(defrecord Alt [rs])

(defn ^:no-doc alt*
  [rs]
  (when-let [rs (remove nil? rs)]
    (cond
      (empty? rs) nil
      (one? rs)   (first rs)
      :else       (->Alt rs))))

(defn alt
  "Create a regex matcher that is the alternation of the given regular
  expressions."
  [& rs]
  (alt* rs))

(defrecord Star [r])

(defn *
  "Match the given regex zero or more times."
  [r] (->Star r))

(defn +
  "Match the given regex one or more times."
  [r]
  (cat r (* r)))

(defrecord Amp [r fs])

(defn &
  "Refine a regex matcher with a any number of predicate functions. Every
  predicate function must return a truthy value. Predicate functions will be
  applied to each token matched by a pattern."
  [r & fs]
  (when-not (every? fn? fs)
    (throw (IllegalArgumentException. "& requires function args")))
  (->Amp r fs))

;; ----------------------------------------------------------------------
;; Logic

(defprotocol Nullable
  (nullable* [this]))

(defprotocol Derivable
  (deriv* [this token]))

(extend-protocol Nullable
  nil
  (nullable* [_] false)

  Accept
  (nullable* [_] true)

  Token
  (nullable* [_] false)

  Concat
  (nullable* [{rs :rs}]
    (every? nullable* rs))

  Alt
  (nullable* [{rs :rs}]
    (boolean (some nullable* rs)))

  Star
  (nullable* [_] true)

  Amp
  (nullable* [{r :r}]
    (nullable* r)))

(extend-protocol Derivable
  nil
  (deriv* [_ _] nil)

  Accept
  (deriv* [_ _] nil)

  Token
  (deriv* [{f :f} token]
    (when (f token)
      accept))

  Concat
  (deriv* [{rs :rs} token]
    (let [r  (first rs)
          rr (rest rs)]
      (if (nullable* r)
        (alt (conscat (deriv* r token) rr) (deriv* (cat* rr) token))
        (conscat (deriv* r token) rr))))

  Alt
  (deriv* [{rs :rs} c]
    (alt* (mapv deriv* rs (repeat c))))

  Star
  (deriv* [{r :r} token]
    (cat (deriv* r token) (* r)))

  Amp
  (deriv* [{r :r, fs :fs} token]
    (when-let [r' (deriv* r token)]
      (when (every? (fn [f] (f token)) fs)
        (if (accept? r') r' (->Amp r' fs))))))

;; ----------------------------------------------------------------------
;; Compiler

(defprotocol Compilable
  (compile* [this]))

(extend-protocol Compilable
  nil
  (compile* [_] nil)

  String
  (compile* [string] (token string))

  Token
  (compile* [token] token)

  java.util.regex.Pattern
  (compile* [re] (regex re))

  clojure.lang.AFunction
  (compile* [f] (predicate f))

  clojure.lang.Sequential
  (compile* [rs] (cat* (mapv compile* rs)))

  Concat
  (compile* [this] (update this :rs (partial mapv compile*)))

  Alt
  (compile* [this] (update this :rs (partial mapv compile*)))

  Amp
  (compile* [this] (update this :r compile*))

  Star
  (compile* [this] (update this :r compile*)))

(deftype Pattern [forms]
  Datafiable
  (datafy [_] forms))

(defn pattern?
  [obj]
  (instance? Pattern obj))

(defn re-pattern
  "Returns a compiled `Pattern` that can be used with `matcher`."
  [forms]
  (if (pattern? forms)
    forms
    (->Pattern (compile* forms))))

;; ----------------------------------------------------------------------
;; API

(def nullable (memoize nullable*))
(def deriv (memoize deriv*))

(defn re-matches
  [re tokens]
  (when (seq tokens)
    (when (nullable (reduce deriv (datafy (re-pattern re)) tokens))
      tokens)))

(defn- accm
  [r path c]
  (vector r (conj path c)))

(defn- ret
  [r path]
  (reduced (vector r path)))

(defn- re-find-step
  [[r path] c]
  (let [r' (delay (deriv r c))]
    (cond
      (accept? r)
      (ret r path)

      (nil? r)
      (ret nil nil)

      (and (nil? @r') (nullable* r) (not-empty path))
      (ret accept path)

      :else
      (accm @r' path c))))

(defn- re-find-first
  [re tokens]
  (when (seq tokens)
    (let [init     [(datafy (re-pattern re)) (vector)]
          [r path] (reduce re-find-step init tokens)]
      (when (nullable* r) path))))

(defn re-find
  [re tokens]
  (loop [tokens tokens]
    (when (seq tokens)
      (if-let [match (re-find-first re tokens)]
        match
        (recur (rest tokens))))))

(defn re-seq
  [re tokens]
  (loop [tokens tokens]
    (when (seq tokens)
      (if-let [match (re-find-first re tokens)]
        (lazy-seq (cons match (re-seq re (drop (count match) tokens))))
        (recur (rest tokens))))))
