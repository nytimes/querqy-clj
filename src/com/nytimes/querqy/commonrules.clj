(ns com.nytimes.querqy.commonrules
  "CommonRules based rewriter"
  (:refer-clojure :exclude [filter])
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [com.nytimes.querqy.model :as model]
   [com.nytimes.querqy.parser :as parser])
  (:import
   (java.io
    Reader)
   (java.net
    URL)
   (java.util
    List
    UUID)
   (querqy.model
    Input
    Input$BooleanInput
    Input$SimpleInput)
   (querqy.parser
    QuerqyParser)
   (querqy.rewrite
    RewriterFactory)
   (querqy.rewrite.commonrules
    CommonRulesRewriter
    LineParser
    QuerqyParserFactory
    SimpleCommonRulesParser
    WhiteSpaceQuerqyParserFactory)
   (querqy.rewrite.commonrules.model
    BoostInstruction
    BoostInstruction$BoostDirection
    BoostInstruction$BoostMethod
    DeleteInstruction
    FilterInstruction
    InstructionDescription
    Instructions
    SynonymInstruction
    TrieMapRulesCollectionBuilder)
   (querqy.rewrite.commonrules.select
    SelectionStrategyFactory)
   (querqy.rewrite.commonrules.select.booleaninput
    BooleanInputParser)
   (querqy.rewrite.commonrules.select.booleaninput.model
    BooleanInputElement
    BooleanInputElement$Type
    BooleanInputLiteral)))

(set! *warn-on-reflection* true)

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
                       ^boolean ignore-case
                       BoostInstruction$BoostMethod/ADDITIVE)]
     (.parse rules-parser))))

(extend-protocol CommonRulesRewriterBuilder
  URL
  (common-rules-rewriter* [url]
    (rewriter-factory (parse-rules (io/reader url)))))

;; ----------------------------------------------------------------------
;; DSL

(def ^:dynamic ^QuerqyParser *query-parser* parser/whitespace-parser)

(defrecord Rule [input instructions])

(defn- description
  ^InstructionDescription
  [& {:keys [^String type, param, ^String value]}]
  (cond-> (InstructionDescription/builder)
    (some? type) (.typeName type)
    (some? param) (.param param)
    (some? value) (.value value)
    :finally (.build)))

(defn match*
  "Create a "
  [head & tail]
  (->Rule head (vec tail)))

(defmacro match
  "Create a rewriter rule from matching text input or boolean input followed by
  any number of query transformations.

  ```clojure
  ;; Inject bar as a synonym to foo in any query.
  (match \"foo\"
    (synonym \"bar\"))
  ```"
  [head & tail]
  `(match* '~head ~@tail))

(defn- parse-string
  [string]
  (mapv #(LineParser/parseTerm %) (str/split string #"\s+")))

(defn- parse-query
  [query]
  (cond
    (string? query)
    (.parse *query-parser* query)

    (map? query)
    (model/rawq {:query query})))

(defn delete?
  [obj]
  (instance? DeleteInstruction obj))

(defn delete
  "Delete text from a matched query. This is useful for removing unhelpful
  search terms from the query."
  [string]
  (DeleteInstruction.
   (parse-string string)
   (description {:type "delete", :value string})))

(defn synonym?
  [obj]
  (instance? SynonymInstruction obj))

(defn synonym
  "Create a synonym for the matched text.

  ```clojure
  (match \"chickpeas\"
    (synonym \"garbanzo beans\"))
  ```"
  ([string]
   (synonym 1.0 string))
  ([boost string]
   (SynonymInstruction.
    (parse-string string)
    boost
    (description {:type "synonym", :param boost, :value string}))))

(defn boost
  "Boost a matching term or query.

  ```clojure
  ;; Boost recipes which take less than 30 minutes when queries
  ;; match quick and recipe or recipes.
  (match (and \"quick\" (or \"recipe\" \"recipes\")
    (boost 10 {:range {:minutes {:lte 30}}}))
  ```"
  [boost query]
  (when (zero? boost)
    (throw (IllegalArgumentException. "Cannot boost by 0")))
  (let [UP   BoostInstruction$BoostDirection/UP
        DOWN BoostInstruction$BoostDirection/DOWN]
    (BoostInstruction.
     (parse-query query)
     (if (>= boost 0) UP DOWN)
     BoostInstruction$BoostMethod/ADDITIVE
     (abs boost)
     (description {:type "boost", :param boost, :value (pr-str query)}))))

(defn filter
  "Add a filter to the query. Filters require a match on documents.

  ```clojure
  (match \"by paul krugman\"
    (filter {:term {:author \"Paul Krugman\"}}))
  ```"
  [query]
  (FilterInstruction.
   (parse-query query)
   (description {:type "filter", :value (pr-str query)})))

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

(defn parse-simple-input
  ^Input$SimpleInput
  [string]
  (Input/parseSimpleInput string))

(extend-protocol CommonRulesRewriterBuilder
  List
  (common-rules-rewriter* [rules]
    (let [rules                (flatten rules)
          counter              (atom 0)
          rules-builder        (TrieMapRulesCollectionBuilder. true)
          boolean-input-parser (BooleanInputParser.)]
      (doseq [{:keys [input instructions]} rules]
        (let [ord          (swap! counter inc)
              id           (str input "#" ord)
              instructions (Instructions. ord id instructions)]
          (cond
            (string? input)
            (.addRule rules-builder (parse-simple-input input) instructions)

            (list? input)
            (do
              (when (some delete? instructions)
                (throw (IllegalArgumentException. "Cannot use a delete instruction with boolean input")))

              (when (some synonym? instructions)
                (throw (IllegalArgumentException. "Cannot use a synonym instruction with boolean input")))

              (let [bool-input (Input$BooleanInput. (parse-boolean-input input) boolean-input-parser (pr-str input))]
                ;;  inputPattern.applyInstructions(instructions, builder);
                (.applyInstructions bool-input instructions rules-builder))))))

      ;; Add boolean literals at the end
      (doseq [^BooleanInputLiteral literal (.values (.getLiteralRegister boolean-input-parser))]
        (let [string (str/join \space (.getTerms literal))
              input  (parse-simple-input string)]
          (.addRule rules-builder input literal)))

      ;;
      (rewriter-factory (.build rules-builder)))))