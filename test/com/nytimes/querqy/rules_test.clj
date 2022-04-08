(ns com.nytimes.querqy.rules-test
  (:refer-clojure :exclude [filter])
  (:require [clojure.test :refer :all]
            [testit.core :refer [facts fact => =in=>]]
            [clojure.datafy :refer [datafy]]
            [com.nytimes.querqy.rules :as r :refer [match synonym boost filter delete]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [com.nytimes.querqy :as querqy])
  (:import (querqy.rewrite.commonrules.select.booleaninput BooleanInputParser)
           (querqy.rewrite.commonrules RuleParseException)
           (querqy.rewrite.commonrules.select.booleaninput.model BooleanInputElement BooleanInputElement$Type)))

(defn parsed
  "Small helper function to convert the list of BooleanInputElements back into sexpr format.
  Helpful for visualizing the parsed output."
  [input]
  (let [parsed-input (r/parse-boolean-input input)
        string-input (apply str (for [^BooleanInputElement token parsed-input]
                                  (cond
                                    (= BooleanInputElement$Type/AND (.-type token))
                                    " AND "

                                    (= BooleanInputElement$Type/OR (.-type token))
                                    " OR "

                                    (= BooleanInputElement$Type/NOT (.-type token))
                                    "NOT "

                                    :else
                                    (.-term token))))]
    (.validateBooleanInput (BooleanInputParser.) parsed-input string-input)
    string-input))

(deftest parse-boolean-input-test
  (is (= "(A OR B)" (parsed '(or "A" "B"))))
  (is (= "(A OR B OR C)" (parsed '(or "A" "B" "C"))))
  (is (= "(A AND B)" (parsed '(and "A" "B"))))
  (is (= "(A AND B AND C)" (parsed '(and "A" "B" "C"))))
  (is (= "(A AND (NOT B))" (parsed '(and "A" (not "B"))))))

;; ----------------------------------------------------------------------
;; Rewriter tests

;; Both of the rewriters we define here should have the same results.

(def resource-rewriter
  (r/rules-rewriter
    (io/resource "com/nytimes/querqy/common-rules.txt")))

(def dsl-rewriter
  (r/rules-rewriter
    ;; basics
    (match "A1" (synonym "B1"))
    (match "A2 B2" (synonym "C2"))
    (match "A3" (synonym "B3") (synonym "C3"))
    (match "A4 B4" (synonym "C4") (synonym "D4"))

    (match "personal computer"
           (synonym "pc")
           (synonym "desktop")
           (boost -50 "software"))))

(defn rewrite
  "util to do a rewrite and datafy the result for easier comparison"
  [rw string]
  (datafy (querqy/rewrite rw string)))

(deftest rewriter-test
  (facts "DSL & Resource Rewriters have the same output"
    (rewrite dsl-rewriter "A1")    => (rewrite resource-rewriter "A1")
    (rewrite dsl-rewriter "A2 B2") => (rewrite resource-rewriter "A2 B2")
    (rewrite dsl-rewriter "A3")    => (rewrite resource-rewriter "A3")
    (rewrite dsl-rewriter "A4 B4") => (rewrite resource-rewriter "A4 B4")))













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
