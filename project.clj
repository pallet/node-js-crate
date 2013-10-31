(defproject com.palletops/node-js-crate "0.8.0-SNAPSHOT"
  :description "Crate for node-js installation"
  :url "http://github.com/pallet/node-js-crate"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [com.palletops/pallet "0.8.0-RC.4"]]
  :resource {:resource-paths ["doc-src"]
             :target-path "target/classes/pallet_crate/node_js_crate/"
             :includes [#"doc-src/USAGE.*"]}
  :prep-tasks ["resource" "crate-doc"])
