(ns nytimes.quip
  (:require
   [clojure.string :as str]
   [nytimes.quip.node.term :refer [term] :as term]
   [nytimes.quip.node.dismax :refer [dismax] :as dismax]
   [nytimes.quip.node.bool :refer [bool should] :as bool]
   [nytimes.quip.node.query :refer [query] :as query]
   [nytimes.quip.node.match-all :refer [match-all]]
   [clojure.java.io :as io]))

;; Goals
;;
;; 1. Allow definining rules in EDN.
;; 2. Allow custom operations to be registered and referred to in EDN.

(defn parse
  "Parse a query string into a query object."
  [string]
  (let [tokens (remove str/blank? (str/split string #"\s+"))]
    (if (seq tokens)
      (query (bool (map (comp should dismax term str/lower-case) tokens)))
      (query (bool (should (match-all)))))))

(defn rewrite
  [ast rules])

(defn emit
  [ast opts])


;; Language

(defn match
  {:style/indent 1}
  [matcher & operations]
  (list :match matcher operations))

(defn synonym
  [x]
  (list :synonym x))

(defn replace
  [x]
  (list :replace x))


(comment

  (import 'java.io.PushbackReader)
  (clojure.edn/read (PushbackReader. (io/reader (io/resource "rules.edn"))))


  (match "guac" (replace "guacamole"))
  (match "cool" (synonym "neat") (synonym "hip"))



;;
)
