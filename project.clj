(defproject metime/metime "0.1.5-SNAPSHOT"
  :description "My Time Tracking App"
  :url "http://metime.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :global-vars {*warn-on-reflection* true}

  :source-paths ["src/clojure" "src"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "1.7" "-source" "1.7" "-Xlint:-options"]
  :plugins [[lein-droid "0.4.3"]]

  :dependencies [[org.clojure-android/clojure "1.7.0-r3"]
                 [neko/neko "4.0.0-alpha5"]]

  :profiles {:default [:dev]

             :dev
             [:android-common :android-user
              {:dependencies [[org.clojure/tools.nrepl "0.2.10" :use-resources true]]
               :target-path "target/debug"
               :android {:aot :all-with-unused
                         :rename-manifest-package "com.gt.metime.debug"
                         :manifest-options {:app-name "MeTime (debug)"}}}]
             :release
             [:android-common
              {:target-path "target/release"
               :android
               { :keystore-path "/Users/GT/.android/private.keystore"
                 :key-alias "greg.play.alias"
                 :sigalg "MD5withRSA"

                :ignore-log-priority [:debug :verbose]
                :aot :all
                :build-type :release}}]}

  :android {;; Specify the path to the Android SDK directory.
            :sdk-path "/Users/GT/Library/Android/sdk"

            ;; Try increasing this value if dexer fails with
            ;; OutOfMemoryException. Set the value according to your
            ;; available RAM.
            :dex-opts ["-JXmx4096M" "--incremental"]

            :target-version "15"
            :aot-exclude-ns ["clojure.parallel" "clojure.core.reducers"
                             "cider.nrepl" "cider-nrepl.plugin"
                             "cider.nrepl.middleware.util.java.parser"
                             #"cljs-tooling\..+"]})
    
