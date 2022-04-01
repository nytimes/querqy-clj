(ns com.nytimes.querqy.util)

(defn private-field
  "Access a private field, for datafying some classes."
  [^Object obj fn-name-string]
  (let [m (.. obj getClass (getDeclaredField fn-name-string))]
    (. m (setAccessible true))
    (. m (get obj))))