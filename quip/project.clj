(defproject com.nytimes/quip "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.11.1"]]
  :repl-options {:init-ns com.nytimes.quip}
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[mvxcvi/puget "1.3.4"]]}})
