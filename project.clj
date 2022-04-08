(defproject com.nytimes/querqy "0.1.0-SNAPSHOT"
  :description "Querqy in Clojure"
  :url "https://github.com/nytimes/querqy-clj"
  :license {:name "Apache License"
            :url  "http://www.apache.org/licenses/LICENSE-2.0"}

  :repositories [["releases" {:url           "https://clojars.org/repo"
                              :sign-releases false
                              :username      [:env/clojars_username]
                              :password      [:env/clojars_password]}]
                 ["snapshots" {:url           "https://clojars.org/repo"
                               :sign-releases false
                               :username      [:env/clojars_username]
                               :password      [:env/clojars_password]}]]

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.querqy/querqy-core "3.12.0"]]

  :repl-options {:init-ns com.nytimes.querqy}
  :global-vars {*warn-on-reflection* true}

  :test-selectors {:default     (complement :integration)
                   :unit        (complement :integration)
                   :integration :integration}

  :profiles {:dev {:source-paths ["dev"]
                   :jvm-opts     ["-Dorg.slf4j.simpleLogger.defaultLogLevel=debug"]
                   :dependencies [[criterium "0.4.6"]
                                  [clj-http "3.12.3"]
                                  [cheshire "5.10.2"]
                                  [metosin/testit "0.4.1"]
                                  [org.slf4j/slf4j-api "1.7.36"]
                                  [org.slf4j/slf4j-simple "1.7.36"]]}}


  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
