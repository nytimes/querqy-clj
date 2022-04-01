(ns com.nytimes.querqy.replace
  "Replace rewriter: https://docs.querqy.org/querqy/rewriters/replace.html"
  (:refer-clojure :exclude [replace])
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str])
  (:import
    (java.io InputStreamReader)
    (java.net URL)
    (java.util List Map UUID)
    (querqy.parser WhiteSpaceQuerqyParser)
    (querqy.rewrite RewriterFactory)
    (querqy.rewrite.contrib ReplaceRewriter)
    (querqy.rewrite.contrib.replace ReplaceRewriterParser TermsReplaceInstruction WildcardReplaceInstruction)
    (querqy.trie SequenceLookup)))

(defprotocol ReplaceRewriterBuilder
  (replace-rewriter* [this]))

(defn replace-rewriter
  "Create a replace rewriter. Accepts one of

   - a URL to a named resource for loading Querqy rules
   - rules specified using the replace DSL

   Examples:

   ```clojure
   (replace-rewriter (io/resource \"my-rules.txt\"))

   (replace-rewriter
     (replace \"foo\" (with \"bar\"))
     (delete \"quux\"))
   ```
   "
  [& args]
  (if (and (= 1 (count args)) (instance? URL (first args)))
    (replace-rewriter* (first args))
    (replace-rewriter* args)))

(defn trie->ReplaceRewriterFactory
  [trie]
  (proxy [RewriterFactory] [(str (UUID/randomUUID))]
    (createRewriter [_ _]
      (ReplaceRewriter. trie))
    (getCacheableGenerableTerms [] #{})))

;; ----------------------------------------------------------------------
;; Resource

(defn parse-stream
  "Parse a stream"
  ([stream]
   (parse-stream stream nil))
  ([stream {:keys [ignore-case, delimiter, parser]
            :or   {ignore-case true
                   delimiter   "\t",
                   parser      (WhiteSpaceQuerqyParser.)}}]
   (let [replace-parser (ReplaceRewriterParser. stream ignore-case delimiter parser)]
     (trie->ReplaceRewriterFactory
       (.parseConfig replace-parser)))))

(extend-protocol ReplaceRewriterBuilder
  URL
  (replace-rewriter* [url]
    (parse-stream (InputStreamReader. (io/input-stream url)))))

;; ----------------------------------------------------------------------
;; Map

(def whitespace #"\s+")
(def wildcard "*")

(defn- flatten-map-keys [m]
  (reduce-kv
    (fn [m k v]
      (if (sequential? k)
        (into m (map vector k (repeat v)))
        (assoc m k v)))
    {} m))

(defn map->SequenceLookup [m]
  (let [trie (SequenceLookup.)]
    (doseq [[input output] (flatten-map-keys m)]
      (let [input  (str/lower-case input)
            output (cond
                     (string? output) (str/split output whitespace)
                     (sequential? output) output
                     (nil? output) [])]
        (cond
          (str/starts-with? input wildcard)
          (if (str/ends-with? input wildcard)
            (throw (ex-info "suffix replace cannot end with wildcard" {:input input, :output output}))
            (.putSuffix
              trie
              (apply str (rest input))
              (WildcardReplaceInstruction. output)))

          (str/ends-with? input wildcard)
          (if (str/starts-with? input wildcard)
            (throw (ex-info "prefix replace cannot end with wildcard" {:input input, :output output}))
            (.putPrefix
              trie
              (apply str (butlast input))
              (WildcardReplaceInstruction. output)))

          :else
          (.put trie [input] (TermsReplaceInstruction. output)))))
    trie))

(extend-protocol ReplaceRewriterBuilder
  Map
  (replace-rewriter* [m]
    (trie->ReplaceRewriterFactory
      (map->SequenceLookup m))))

;; ----------------------------------------------------------------------
;; DSL

(defmacro replace
  "Replace the given term with another.

  Example:

  ```clojure
  (replace \"foos\" (with \"foo\"))
  ```
  "
  {:style/indent 1}
  [input output]
  `(let [input# '~input]
     (if-not (list? input#)
       (list [~input ~output])
       (case (str (first input#))
         "or" (map vector (rest input#) (repeat ~output))
         (throw (IllegalArgumentException. (str "Illegal input: " (pr-str input#))))))))

(defn with
  "Intended to be used with replace. This is the target for a replace rule."
  [output]
  output)

(defn delete
  "A replace rule to delete the term from any user query. This basically expands to

  ```clojure
  (replace \"foo\" (with nil))
  ```
  "
  [input]
  (replace input (with nil)))

(extend-protocol ReplaceRewriterBuilder
  List
  (replace-rewriter* [rule-list]
    (replace-rewriter* (into {} (apply concat rule-list)))))
