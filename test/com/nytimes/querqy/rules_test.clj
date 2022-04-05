(ns com.nytimes.querqy.rules-test
  (:refer-clojure :exclude [filter])
  (:require [clojure.test :refer :all]
            [testit.core :refer [facts ]]
            [clojure.datafy :refer [datafy]]
            [com.nytimes.querqy.rules :as r :refer [match synonym boost filter delete]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [com.nytimes.querqy :as querqy])
  (:import (querqy.rewrite.commonrules.select.booleaninput BooleanInputParser)
           (querqy.rewrite.commonrules RuleParseException)))

(defn validate-boolean-input [bool-input string]
  (try
    (.validateBooleanInput (BooleanInputParser.) bool-input string)
    true
    (catch RuleParseException _ false)))


(defn boolean-input->string*
  [])

(deftest boolean-input->string
  (let [parse-input (fn )])
  (are [input expected]
    (testing input
      (let [actual (r/boolean-input->string input)]
        (is (= expected actual))
        (is (try (.validateBooleanInput (BooleanInputParser.)
                                        (r/parse-boolean-input input)
                                        actual)
                 true
                 (catch RuleParseException _ false)))
        ))
    ;;
    '(or "a" "b")
    "(a OR b)"

    '(and "a" "b")
    "(a AND b)"
    )
  )


;
;(def input '(or "a" (not "b")))
;(def parsed (r/parse-boolean-input input))
;(def actual (r/boolean-input->string input))
;(.validateBooleanInput (BooleanInputParser.) parsed actual)





(defn query->string
  [q]
  (->> (datafy q)
       :userQuery
       (map :clause)
       (mapcat identity)
       (str/join " ")
       (str/trim)))

(def resource-rewriter
  (r/rules-rewriter
    (io/resource "com/nytimes/querqy/common-rules.txt")))

;personal computer =>
;SYNONYM: pc
;SYNONYM: desktop computer
;DOWN(50): software
;
;iphone AND NOT case =>
;UP(50): * category:mobilephone
;
;(def dsl-rewriter
;  (r/rules-rewriter
;    (match "personal computer"
;           (synonym "pc")
;           (synonym "desktop computer")
;           (boost -50 "software"))
;    (match (and "iphone" (not "case"))
;           (boost 50 {:term {:category "mobilephone"}}))))
;
;(deftest resource-config
;  (is (= "foo 123" (query->string (querqy/rewrite resource-rewriter "foos 123"))))
;  (is (= "bar or bar" (query->string (querqy/rewrite resource-rewriter "bars or barz"))))
;  (is (= "delete the" (query->string (querqy/rewrite resource-rewriter "delete the quux")))))
;
;(deftest dsl-config
;  (is (= "foo 123" (query->string (querqy/rewrite dsl-rewriter "foos 123"))))
;  (is (= "bar or bar" (query->string (querqy/rewrite dsl-rewriter "bars or barz"))))
;  (is (= "delete the" (query->string (querqy/rewrite dsl-rewriter "delete the quux")))))
;
