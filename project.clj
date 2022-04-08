(defproject com.nytimes/querqy "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.querqy/querqy-core "3.12.0"]]
  :repl-options {:init-ns com.nytimes.querqy}
  :global-vars {*warn-on-reflection* true}

  :test-selectors {:default     (complement :integration)
                   :unit        (complement :integration)
                   :integration :integration}

  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[criterium "0.4.4"]
                                  [clj-http "3.12.3"]
                                  [cheshire "5.10.1"]
                                  [metosin/testit "0.4.1"]
                                  [org.slf4j/slf4j-simple "1.7.32"]]}})
