(ns com.nytimes.querqy.regex-test
  (:refer-clojure :exclude [compile cat re-pattern * + &])
  (:require
   [clojure.test :refer [are deftest is testing]]
   [com.nytimes.querqy.regex :as r :refer [* + & accept alt cat deriv nullable pattern? predicate regex token]]
   [matcher-combinators.core :as mc]
   [matcher-combinators.model :as model]
   [matcher-combinators.test :refer [match?]]
   [clojure.string :as str]))

;; tokens as if parsed from text

(defn t [s] {:text s})

(def a (t "a"))
(def b (t "b"))
(def c (t "c"))

(defn lower? [{:keys [text]}]
  (if (str/blank? text)
    false
    (reduce (fn [_ c] (if (Character/isLowerCase c) true (reduced false)))
            nil
            text)))

(defn upper? [{:keys [text]}]
  (if (str/blank? text)
    false
    (reduce (fn [_ c] (if (Character/isUpperCase c) true (reduced false)))
            nil
            text)))

;; matchers to match tokens

(def a? (r/token "a"))
(def b? (r/token "b"))
(def c? (r/token "c"))

(defn regex-equals
  "Custom matcher to check regex equality for these tests."
  [expected]
  (reify mc/Matcher
    (-base-name [_] 'regex-equals)
    (-matcher-for [this] this)
    (-matcher-for [this _] this)
    (-match [_ actual]
      (if (= expected actual)
        {:matcher-combinators.result/type   :match
         :matcher-combinators.result/value  actual
         :matcher-combinators.result/weight 0}
        {:matcher-combinators.result/type   :mismatch
         :matcher-combinators.result/value  (model/->Mismatch expected actual)
         :matcher-combinators.result/weight 1}))))

;; ----------------------------------------------------------------------
;; Tests

(deftest nullable-test
  (are [x m] (match? m (nullable x))
    ;; value
    nil                 false
    accept              true
    (token "a")         false
    (regex #"abc")      false
    (predicate int?)    false
    ;; cat
    (cat accept)        true
    (cat a? b?)         false
    ;; alt
    (alt accept b?)     true
    (alt a? accept)     true
    (alt a? a? accept)  true
    ;; amp
    (& accept lower?) true
    ;; (& accept t/upper?) false
    ;; star
    (* a?)              true
    (* (cat a? b?))     true
    ;; plus
    (+ a?)              false
    (+ accept)          true))

(deftest deriv-test
  ;; All these patterns are derived w.r.t. the token "a".
  (are [x m] (match? (regex-equals m) (deriv x a))
    ;; value
    nil                      nil
    accept                   nil
    a?                       accept
    b?                       nil
    (regex #"a")             accept
    (regex #"\w")            accept
    (regex #"\d")            nil
    (predicate lower?)     accept
    (predicate upper?)     nil
    ;; cat
    (cat a? b?)              b?
    (cat a? b? c?)           (cat b? c?)
    (cat (* a?) b?)          (cat (* a?) b?)
    (cat (* b?) a?)          accept
    (cat (* b?) a? c?)       c?
    ;; alt
    (alt a? b?)              accept
    (alt b? a?)              accept
    (cat (alt a? b?) c?)     c?
    ;; amp
    (& a? lower?)          accept
    (& a? upper?)          nil
    (& (cat a? b?) lower?) (& b? lower?)
    ;; star
    (* a?)                   (* a?)
    (* b?)                   nil
    (* (cat a? b?))          (cat b? (* (cat a? b?)))
    ;; plus
    (+ a?)                   (* a?)))

(deftest compiler-test
  (are [input] (match? pattern? (r/re-pattern input))
    "abc"
    #"abc"
    upper?
    ["a" "b"]
    (cat a? b?)
    (cat (alt "a" "b") (alt "c" "d"))
    (* a?)
    (+ b?)
    (& a? lower?)))

(deftest matches-test
  (are [pattern tokens] (match? tokens (r/re-matches pattern tokens))
    a?                                    [a]
    (cat a? a?)                           [a a]
    (cat a? b?)                           [a b]
    (cat a? (alt b? c?))                  [a b]
    (cat a? (alt b? c?))                  [a c]
    (cat (* a?) b? c?)                    [a b c]
    (cat (+ a?) b?)                       [a a a a b]
    (cat b? a? (* (cat b? a?)))           [b a]
    (cat b? a? (* (cat b? a?)))           [b a b a]
    (cat b? a? (* (cat b? a?)))           [b a b a b a b a b a b a b a]
    (alt (cat a? (+ b?)) (cat b? (+ a?))) [a b b b b b]
    (alt (cat a? (+ b?)) (cat b? (+ a?))) [b a a a a a])

  (testing "kleene star does not match empty document"
    (is (match? nil (r/re-matches (* a?) [])))))

(deftest find-test
  (are [actual expected] (match? expected actual)
    (r/re-find a? [a])                        [a]
    (r/re-find a? [b])                        nil
    (r/re-find a? [a a a a])                  [a]
    (r/re-find a? [b a])                      [a]
    (r/re-find (cat a? b?) [a b])             [a b]
    (r/re-find (cat a? b?) [b a])             nil
    (r/re-find (cat a? b? c?) [a a b c])      [a b c]
    (r/re-find (alt a? b?) [a])               [a]
    (r/re-find (alt a? b?) [b])               [b]
    (r/re-find (cat (+ a?) b?) [a a a b a b]) [a a a b]
    (r/re-find (& a? lower?)  [b b a])      [a]
    (r/re-find (* a?) [a a b])                [a a]
    (r/re-find (* (alt a? b?)) [c a b c])     [a b]
    (r/re-find (* (alt a? b?)) [c b a c])     [b a]
    (r/re-find (cat a? b?) [a c b a b])       [a b]
    (r/re-find (+ (cat a? b?)) [a b c])       [a b]
    (r/re-find (+ a?) [a a a a])              [a a a a]
    (r/re-find (cat a? b?) [a c b])           nil))

(deftest find-seq-test
  (are [actual expected] (match? expected actual)
    (r/re-seq a? [a a a a])
    [[a] [a] [a] [a]]

    (r/re-seq (cat a? b?) [a c b a b])
    [[a b]]

    (r/re-seq (+ a?) [a a a a])
    [[a a a a]]

    (r/re-seq (+ (cat a? b?)) [a b c a b a b c])
    [[a b]
     [a b a b]]

    (r/re-seq (alt (cat a? b?) (cat b? c?)) [a c b c a b])
    [[b c]
     [a b]]

    (r/re-seq (+ a?) [a a a a b a a a a])
    [[a a a a]
     [a a a a]]

    (r/re-seq (* a?) [])
    nil

    (r/re-seq (* a?) [a b b b b b b b a])
    [[a]
     [a]]

    (r/re-seq (& a? lower?) [a b b b b b b b a])
    [[a]
     [a]]))
