(defproject com.nytimes/quip "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.11.1"]]
  :repl-options {:init-ns nytimes.quip}
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[mvxcvi/puget "1.3.4"]
                                  [lambdaisland/kaocha "1.82.1306"]]}}
  :aliases {"kaocha" ^:pass-through-help ["run" "-m" "kaocha.runner"]})
