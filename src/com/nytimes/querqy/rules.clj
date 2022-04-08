(ns com.nytimes.querqy.rules
  "CommonRules based rewriter"
  (:refer-clojure :rename {filter cfilter})
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.nytimes.querqy.model :as model]
            [com.nytimes.querqy.parser :as parser])
  (:import (java.io Reader)
           (querqy.rewrite.commonrules QuerqyParserFactory WhiteSpaceQuerqyParserFactory SimpleCommonRulesParser CommonRulesRewriter LineParser)
           (java.net URL)
           (querqy.rewrite.commonrules.select SelectionStrategyFactory)
           (java.util UUID List)
           (querqy.rewrite RewriterFactory)
           (querqy.rewrite.commonrules.model TrieMapRulesCollectionBuilder Instruction Instructions BoostInstruction$BoostDirection BoostInstruction DeleteInstruction SynonymInstruction FilterInstruction)
           (querqy.model Input Input$SimpleInput Input$BooleanInput)
           (querqy.rewrite.commonrules.select.booleaninput BooleanInputParser)
           (querqy.rewrite.commonrules.select.booleaninput.model BooleanInputElement BooleanInputElement$Type BooleanInputLiteral)
           (clojure.lang Symbol)
           (querqy.parser QuerqyParser)))

(defprotocol CommonRulesRewriterBuilder
  (common-rules-rewriter* [this]))

(extend-protocol CommonRulesRewriterBuilder
  nil
  (common-rules-rewriter* [_]
    (throw (IllegalArgumentException. "Must provide rules to rules-rewriter"))))

(defn rules-rewriter
  "Create a CommonRulesRewriter.

  You may pass in a resource pointing to a Querqy CommonRulesRewriter file or
  use the Clojure-DSL."
  [& args]
  (if (and (= 1 (count args)) (instance? URL (first args)))
    (common-rules-rewriter* (first args))
    (common-rules-rewriter* args)))

(defn rewriter-factory
  [rules]
  (proxy [RewriterFactory] [(str (UUID/randomUUID))]
    (createRewriter [_ _]
      (CommonRulesRewriter.
       rules
       SelectionStrategyFactory/DEFAULT_SELECTION_STRATEGY))
    (getCacheableGenerableTerms [] #{})))

;; ----------------------------------------------------------------------
;; Resource

(defn parse-rules
  ([stream]
   (parse-rules stream nil))
  ([^Reader stream {:keys [boolean-input, ignore-case, parser]
                    :or   {boolean-input true
                           ignore-case   true
                           parser        (WhiteSpaceQuerqyParserFactory.)}}]
   (let [rules-parser (SimpleCommonRulesParser.
                       ^Reader stream
                       ^boolean boolean-input
                       ^QuerqyParserFactory parser
                       ^boolean ignore-case)]
     (.parse rules-parser))))

(extend-protocol CommonRulesRewriterBuilder
  URL
  (common-rules-rewriter* [url]
    (rewriter-factory (parse-rules (io/reader url)))))

;; ----------------------------------------------------------------------
;; DSL

(extend-protocol CommonRulesRewriterBuilder
  List
  (common-rules-rewriter* [rules]
    (let [rules-builder (TrieMapRulesCollectionBuilder. true)]
      (doseq [rule-fn rules]
        (rule-fn rules-builder))
      (rewriter-factory (.build rules-builder)))))

(def ^:dynamic ^QuerqyParser *query-parser* parser/whitespace-parser)

(declare match*)

(defmacro match
  "Create a match rule."
  {:style/indent 1}
  ;; TODO LEFT/ RIGHT boundaries
  [head & tail]
  `(match* '~head (vector ~@tail)))

(defn- parse-string [string] (mapv #(LineParser/parseTerm %) (str/split string #"\s+")))

(defn- parse-query [query]
  (cond (string? query) (.parse *query-parser* query)
        (map? query) (model/rawq {:query query})))

(defn delete? [obj] (instance? DeleteInstruction obj))

(defn delete [string] (DeleteInstruction. (parse-string string)))

(defn synonym? [obj] (instance? SynonymInstruction obj))

(defn synonym
  "Create a synonym instruction."
  ([string]
   (SynonymInstruction. (parse-string string)))
  ([boost string]
   (SynonymInstruction. (parse-string string) boost)))

(defn boost
  [boost query]
  (when (zero? boost)
    (throw (IllegalArgumentException. "Cannot boost by 0")))
  (let [UP   BoostInstruction$BoostDirection/UP
        DOWN BoostInstruction$BoostDirection/DOWN]
    (BoostInstruction. (parse-query query)
                       (if (>= boost 0) UP DOWN)
                       (abs boost))))

(defn filter
  [query]
  (FilterInstruction. (parse-query query)))

;;; match impl

(defmulti parse-boolean-input (fn [form] (type form)))

(defmethod parse-boolean-input String
  [string]
  (for [term (str/split string #"\s+")]
    (BooleanInputElement. term BooleanInputElement$Type/TERM)))

(defmethod parse-boolean-input List
  [[operator & terms :as input]]
  (let [OR     (BooleanInputElement. "OR" BooleanInputElement$Type/OR)
        AND    (BooleanInputElement. "AND" BooleanInputElement$Type/AND)
        NOT    (BooleanInputElement. "NOT" BooleanInputElement$Type/NOT)
        LEFTP  (BooleanInputElement. "(" BooleanInputElement$Type/LEFT_PARENTHESIS)
        RIGHTP (BooleanInputElement. ")" BooleanInputElement$Type/RIGHT_PARENTHESIS)
        wrap   (fn [xs] (flatten (concat (list LEFTP) xs (list RIGHTP))))]
    (case operator
      or (wrap (interpose OR (parse-boolean-input terms)))
      and (wrap (interpose AND (parse-boolean-input terms)))
      not (wrap (cons NOT (parse-boolean-input terms)))
      (map parse-boolean-input input))))

;;

(def rule-count (atom 0))

(defn ^:no-doc match*
  ;; implements match for use by match macro
  [input instructions]
  (let [ord      (swap! rule-count inc)
        compiled (Instructions. ord ord instructions)]
    (fn [^TrieMapRulesCollectionBuilder rules-builder]
      (cond
        ;; string rules
        (string? input)
        (let [simple-input (Input/parseSimpleInput input)]
          (.addRule rules-builder
                    ^Input$SimpleInput simple-input
                    ^Instructions compiled))

        ;; boolean rules
        (list? input)
        (do
          (when (some delete? instructions)
            (throw (IllegalArgumentException. "Cannot use a delete instruction with boolean input")))
          (when (some synonym? instructions)
            (throw (IllegalArgumentException. "Cannot use a synonym instruction with boolean input")))
          (let [boolean-input-parser (BooleanInputParser.)
                bool-input           (Input$BooleanInput. (parse-boolean-input input)
                                                          boolean-input-parser
                                                          (pr-str input))]

            (.applyInstructions bool-input compiled rules-builder)
            (doseq [^BooleanInputLiteral literal (.values (.getLiteralRegister boolean-input-parser))]
              (let [input (LineParser/parseInput (str/join \space (.getTerms literal)))]
                (.addRule rules-builder
                          ^Input$SimpleInput input
                          ^BooleanInputLiteral literal)))))

        :else (throw (IllegalArgumentException. "Can only parse a string or list as input"))))))
