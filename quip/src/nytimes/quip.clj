(ns nytimes.quip)


(defn parse
  [string])

(defn rewrite
  [ast rules])

(defn emit
  [ast opts])

(comment
  (def rules)
  (def options)
  (-> (parse "guac recipes")
      (rewrite rules)
      (emit options))

  )
