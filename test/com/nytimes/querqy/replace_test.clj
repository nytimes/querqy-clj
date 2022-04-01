(ns com.nytimes.querqy.replace-test
  (:refer-clojure :exclude [replace])
  (:require
   [clojure.datafy :refer [datafy]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [are deftest is]]
   [com.nytimes.querqy.datafy]
   [com.nytimes.querqy :as querqy]
   [com.nytimes.querqy.replace :as r :refer [delete replace with]]))

(defn query->string
  [q]
  (->> (datafy q)
       :userQuery
       (map :clause)
       (mapcat identity)
       (str/join " ")
       (str/trim)))

(def map-rewriter
  (r/replace-rewriter
   {"foos"          "foo"
    ["bars" "barz"] "bar"
    "quux"          ""}))

(def resource-rewriter
  (r/replace-rewriter
   (io/resource "com/nytimes/querqy/replace-rules.txt")))

(def dsl-rewriter
  (r/replace-rewriter
   (replace "foos" (with "foo"))
   (replace (or "bars" "barz") (with "bar"))
   (delete "quux")))

(deftest map-config
  (is (= "foo 123"    (query->string (querqy/rewrite map-rewriter "foos 123"))))
  (is (= "bar or bar" (query->string (querqy/rewrite map-rewriter "bars or barz"))))
  (is (= "delete the" (query->string (querqy/rewrite map-rewriter "delete the quux")))))

(deftest resource-config
  (is (= "foo 123"    (query->string (querqy/rewrite resource-rewriter "foos 123"))))
  (is (= "bar or bar" (query->string (querqy/rewrite resource-rewriter "bars or barz"))))
  (is (= "delete the" (query->string (querqy/rewrite resource-rewriter "delete the quux")))))

(deftest dsl-config
  (is (= "foo 123"    (query->string (querqy/rewrite dsl-rewriter "foos 123"))))
  (is (= "bar or bar" (query->string (querqy/rewrite dsl-rewriter "bars or barz"))))
  (is (= "delete the" (query->string (querqy/rewrite dsl-rewriter "delete the quux")))))

(def rewriter
  (r/replace-rewriter
   (replace "cheap*"
     (with "cheap"))
   (replace "samrt*"
     (with "smart$1"))
   (replace "computer*"
     (with "computer $1"))
   (replace (or "*phones" "*hpone" "*hpones")
     (with "$1phone"))
   (replace "*+"
     (with "$1 plus"))
   (replace "*."
     (with "$1"))
   (replace "*)"
     (with "$1"))
   (replace "(*"
     (with "$1"))))

(deftest replace-test
  (are [input output] (= output (query->string (querqy/rewrite rewriter input)))
    "cheapest cheaper cheap" "cheap cheap cheap"
    "samrtphone"             "smartphone"
    "computerscreen"         "computer screen"
    "iphones"                "iphone"
    "ihpone ihpone"          "iphone iphone"
    "galaxy+ phone"          "galaxy plus phone"
    "The Batman (2022)"      "The Batman 2022"))
