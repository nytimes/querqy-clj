(ns user
  (:require
   [puget.printer :refer [cprint]]
   [nytimes.quip.node :as node]
   [nytimes.quip.zip :as z]
   [clojure.string :as str]
   [clojure.test :refer [deftest is are testing]]
   [clojure.zip :as cz]))

(defn parse
  [string]
  (node/query
   (node/bool
    (for [token (str/split string #"\s+")]
      (node/should (node/dismax (node/term token)))))))

(testing "parsed query structure"
  (is (= (node/query
          (node/bool
           (list (node/should (node/dismax (node/term "hello")))
                 (node/should (node/dismax (node/term "world"))))))
         (parse "hello world"))))


(def root (z/zipper (parse "hello world")))

(defn synonym
  [zloc term syn]
  (-> (z/find-node zloc (partial node/term= term))
      (z/up)
      (z/edit update :children conj syn)))

(defn prune
  [zloc]
  (->> zloc
      (iterate z/prev)
      (take-while (fn [node] (println "-----") (cprint node) node))

      ))

(defn delete
  [zloc term]
  (z/edit-all zloc (partial node/term= term) z/next)
  (-> (z/find-node zloc (partial node/term= term))
      (z/remove)
      (z/up)
      (z/edit prn)))

(testing "synonyms"
  (is (= (node/query
          (node/bool
           (list (node/should (node/dismax (list (node/term "hi") (node/term "hello"))))
                 (node/should (node/dismax (node/term "world"))))))
         (-> root
             (synonym (node/term "hello") (node/term "hi"))
             (z/root)))))

(testing "delete"
  (is (= (node/query
          (node/bool
           (list (node/should (node/dismax (node/term "a")))
                 (node/should (node/dismax (node/term "b")))
                 (node/should (node/dismax (node/term "d"))))))
         (z/root (delete (z/zipper (parse "a b c d"))
                         (node/term "c"))))))





(comment
  (cprint (z/root (synonym root (node/term "hello") (node/term "hi"))))
  (cprint (-> root
              (synonym (node/term "hello") (node/term "hi"))
              (delete (node/term "world"))))

  )
