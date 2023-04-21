(ns nytimes.quip.zip-test
  (:require
   [nytimes.quip.zip :as z]
   [nytimes.quip.node.query :refer [query]]
   [nytimes.quip.node.dismax :refer [dismax]]
   [nytimes.quip.node.bool :refer [bool should must]]
   [nytimes.quip.node.term :refer [term] :as term]
   [clojure.test :refer [deftest is are testing]]
   [clojure.string :as str]))

(deftest node-next-test ;; todo protocol basics
  (testing "dismax"
    (let [zloc (z/zipper (dismax (term "a") (term "b")))]
      (is (= (term "a") (-> zloc z/next z/node)))
      (is (= (term "b") (-> zloc z/next z/next z/node)))))

  (testing "bool"
    (let [zloc (z/zipper (bool (should (dismax (term "a") (term "b")))))]
      (is (= (term "a") (-> zloc z/next z/next z/next z/node)))
      (is (= (term "b") (-> zloc z/next z/next z/next z/next z/node)))))

  (testing "query"
    (let [zloc (z/zipper (query (bool (should (dismax (term "a") (term "b"))))))]
      (is (= (term "a") (-> zloc z/next z/next z/next z/next z/node)))
      (is (= (term "b") (-> zloc z/next z/next z/next z/next z/next z/node))))))


;;(deftest base-test)


(deftest movement-test
  (testing "down"
    (is (= (dismax (term "a") (term "b"))
           (-> (z/zipper (bool (should (dismax (term "a") (term "b")))))
               z/down
               z/down
               z/node))))

  (testing "up"
    (is (= (should (dismax (term "a") (term "b")))
           (-> (z/zipper (bool (should (dismax (term "a") (term "b")))))
               z/down
               z/down
               z/up
               z/node))))

  (testing "next"
    (is (= (term "b")
           (-> (z/zipper (dismax (term "a") (term "b")))
               z/next
               z/next
               z/node))))

  (testing "prev"
    (is (= (term "a")
           (-> (z/zipper (dismax (term "a") (term "b")))
               z/next
               z/next
               z/prev
               z/node))))

  (testing "right")

  (testing "rightmost")

  (testing "left")

  (testing "leftmost")

  (let [zloc (z/zipper (dismax (term "a") (term "b")))]
    (is (= zloc (-> zloc z/down z/right z/left z/up))))

  (let [zloc (z/zipper (dismax (term "a") (term "b") (term "c")))]
    (is (= zloc (-> zloc z/down z/rightmost z/leftmost z/up))))

  (let [zloc (z/zipper (dismax (term "a") (term "b") (term "c")))]
    (is (= (term "c") (-> zloc z/down z/rightmost z/node)))))


(def term? (comp term/term? z/node))

(deftest finding-test
  (testing "find"
    (testing "find first term"
      (is (= (term "a")
             (some-> (z/zipper (bool (should (dismax (term "a")))
                                     (should (dismax (term "b")))
                                     (should (dismax (term "c")))))
                     (z/find term?)
                     (z/node)))))

    (testing "find second term"
      (is (= (term "b")
             (some-> (z/zipper (bool (should (dismax (term "a")))
                                     (should (dismax (term "b")))
                                      (should (dismax (term "c")))))
                 (z/find (fn [zloc] (and term? (= "b" (:text (z/node zloc))))))
                 (z/node)))))

    (testing "find no matches"
      (is (= nil
             (some-> (z/zipper (bool (should (dismax (term "a")))
                                     (should (dismax (term "b")))
                                     (should (dismax (term "c")))))
                     (z/find (fn [zloc] (and term? (= "missing" (:text (z/node zloc))))))
                     (z/node))))))

  (testing "find-next"
    (is (= (term "a")
           (some-> (z/zipper (bool (should (dismax (term "a")))
                                     (should (dismax (term "b")))
                                     (should (dismax (term "c")))))
                   (z/find-next term?)
                   (z/node))))

    (is (= (term "b")
           (some-> (z/zipper (bool (should (dismax (term "a")))
                                   (should (dismax (term "b")))
                                   (should (dismax (term "c")))))
                   (z/find-next term?)
                   (z/find-next term?)
                   (z/node))))

    (is (= (term "c")
           (some-> (z/zipper (bool (should (dismax (term "a")))
                                   (should (dismax (term "b")))
                                   (should (dismax (term "c")))))
                   (z/find-next term?)
                   (z/find-next term?)
                   (z/find-next term?)
                   (z/node)))))

  (testing "find-all"
    (testing "finding all terms"
      (is (= [(term "a") (term "b") (term "c")]
             (some-> (z/zipper (bool (should (dismax (term "a")))
                                     (should (dismax (term "b")))
                                     (should (dismax (term "c")))))
                     (z/find-all term?)
                     (->> (map z/node))))))

    (testing "no matches"
      (is (= []
             (some-> (z/zipper (bool (should (dismax (term "a")))
                                     (should (dismax (term "b")))
                                     (should (dismax (term "c")))))
                     (z/find-all (fn [zloc] (and (term? zloc) (= "missing" (:text (z/node zloc))))))))))))


(deftest edit-test
  (testing "append-child"
    (is (= (dismax (term "a") (term "b"))
           (some-> (z/zipper (dismax (term "a")))
                   (z/append-child (term "b"))
                   (z/root)))))

  (testing "edit"
    (is (= (dismax (term "A"))
           (some-> (z/zipper (dismax (term "a")))
                   (z/down)
                   (z/edit update :text str/capitalize)
                   (z/root)))))

  (testing "edit-all"
    (is (= (dismax (term "A") (term "B") (term "C"))
           (some-> (z/zipper (dismax (term "a") (term "b") (term "c")))
                   (z/down)
                   (z/edit-all term? (fn [zloc] (update zloc :text str/capitalize)))
                   (z/root)))))

  (testing "remove"
    (is (= (dismax (term "a") (term "c"))
           (some-> (z/zipper (dismax (term "a") (term "b") (term "c")))
                   (z/down)
                   (z/next)
                   (z/remove)
                   (z/root)))))

  (testing "replace"
    (is (= (dismax (term "a") (term "B") (term "c"))
           (some-> (z/zipper (dismax (term "a") (term "b") (term "c")))
                   (z/down)
                   (z/next)
                   (z/replace (term "B"))
                   (z/root))))))
