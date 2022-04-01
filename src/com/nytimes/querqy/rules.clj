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

(defn rules-rewriter [& args]
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
    (rewriter-factory
      (parse-rules
        (io/reader url)))))

(comment

  (clojure.datafy/datafy
    (querqy/rewrite
      (rules-rewriter (io/resource "com/nytimes/querqy/common-rules.txt"))
      "dell personal computer")))


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

(defmacro match [head & tail]
  `(match* '~head (vector ~@tail)))

(defn- parse-string [string] (mapv #(LineParser/parseTerm %) (str/split string #"\s+")))

(defn- parse-query [query]
  (cond (string? query) (.parse *query-parser* query)
        (map? query) (model/rawq {:query query})))

(defn delete
  [string] (DeleteInstruction. (parse-string string)))

(defn synonym
  ([string]
   (SynonymInstruction. (parse-string string)))
  ([boost string]
   (SynonymInstruction. (parse-string string) boost)))

(defn boost
  [boost query]
  (let [UP   BoostInstruction$BoostDirection/UP
        DOWN BoostInstruction$BoostDirection/DOWN]
    (BoostInstruction. (parse-query query)
                       (if (>= boost 0) UP DOWN)
                       boost)))

(defn filter
  [query]
  (FilterInstruction. (parse-query query)))

;;; match impl

(defmulti parse-boolean-input (fn [form] (type form)))

(defmethod parse-boolean-input String
  [string]
  (vec
    (for [term (str/split string #"\s+")]
      (BooleanInputElement. term BooleanInputElement$Type/TERM))))

(defmethod parse-boolean-input List
  [[operator & terms]]
  (let [OR         (BooleanInputElement. "OR" BooleanInputElement$Type/OR)
        AND        (BooleanInputElement. "AND" BooleanInputElement$Type/AND)
        NOT        (BooleanInputElement. "NOT" BooleanInputElement$Type/NOT)
        LEFTP      (BooleanInputElement. "(" BooleanInputElement$Type/LEFT_PARENTHESIS)
        RIGHTP     (BooleanInputElement. ")" BooleanInputElement$Type/RIGHT_PARENTHESIS)
        boolean-op (case (str operator)
                     "or" OR
                     "and" AND
                     "not" NOT)]
    (vec
      (concat (list LEFTP)
              (butlast (interleave (mapcat parse-boolean-input terms) (repeat boolean-op)))
              (list RIGHTP)))))

;;

(defmulti boolean-input->string (fn [form] (type form)))

(defmethod boolean-input->string List
  [form]
  (let [wrap     (fn [x] (format "(%s)" x))
        operator (str (first form))]
    (case operator
      "or" (wrap (str/join " OR " (mapv boolean-input->string (rest form))))
      "and" (wrap (str/join " AND " (mapv boolean-input->string (rest form))))
      "not" (str "NOT " (wrap (mapv boolean-input->string (rest form)))))))

(defmethod boolean-input->string Symbol [form] (str form))
(defmethod boolean-input->string String [form] form)

;;

(def rule-count (atom 0))

(defn match* [input instructions]
  (let [ord          (swap! rule-count inc)
        instructions (Instructions. ord ord instructions)]
    (fn [^TrieMapRulesCollectionBuilder rules-builder]
      (cond
        ;; string rules
        (string? input)
        (let [simple-input (Input/parseSimpleInput input)]
          (.addRule rules-builder
                    ^Input$SimpleInput simple-input
                    ^Instructions instructions))

        ;; boolean rules
        (list? input)
        (let [boolean-input-parser (BooleanInputParser.)
              bool-input           (Input$BooleanInput. (parse-boolean-input input)
                                                        boolean-input-parser
                                                        (boolean-input->string input))]

          (.applyInstructions bool-input instructions rules-builder)
          (doseq [^BooleanInputLiteral literal (.values (.getLiteralRegister boolean-input-parser))]
            (let [input (LineParser/parseInput (str/join \space (.getTerms literal)))]
              (.addRule rules-builder
                        ^Input$SimpleInput input
                        ^BooleanInputLiteral literal))))


        :else (throw (IllegalArgumentException. "Can only parse a string or list as input"))))))



(comment

  (def builder (TrieMapRulesCollectionBuilder. true))
  (def m (match "foo" (boost 2 "bar")))
  (m builder)
  ((match* "foo" [(boost 2 "bar")]) builder)
  ((match* '(or "baz quuz") [(boost 2 "bar")]) builder)

  (def rw (rewriter-factory (.build builder)))

  )




;; ----------------------------------------------------------------------
;; Datafy