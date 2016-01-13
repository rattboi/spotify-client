(defproject spotify-client "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"] 
                 [compojure "1.4.0"]
                 [clj-http "2.0.0"]
                 [cheshire "5.5.0"]
                 [noir "1.3.0"]]
  :main ^:skip-aot spotify-client.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
