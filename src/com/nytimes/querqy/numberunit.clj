(ns com.nytimes.querqy.numberunit
  "Number Unit rewriter, see

  https://docs.querqy.org/querqy/rewriters/number-unit.html"
  (:import
   (querqy.rewrite.contrib NumberUnitRewriter NumberUnitRewriterFactory)
   (querqy.rewrite.contrib.numberunit NumberUnitQueryCreator)
   (querqy.rewrite.contrib.numberunit.model NumberUnitDefinition)
   (querqy.rewrite RewriterFactory)
   (java.util UUID))
  (:require
   [clojure.spec.alpha :as s]))


(comment
  (NumberUnitRewriterFactory. (str (UUID/randomUUID))
                              []
                              nil)

  (defn numberunit-rewriter
    []
    (let [numberunit-map nil
          numberunit-query-creator nil]
      (proxy [RewriterFactory] [(str (UUID/randomUUID))]
        (createRewriter [_ _]
          (NumberUnitRewriter. numberunit-map numberunit-query-creator))
        (getCacheableGenerableTerms [] #{}))))

  ,)
