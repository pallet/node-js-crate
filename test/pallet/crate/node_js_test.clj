(ns pallet.crate.node-js-test
  (:require
   [clojure.test :refer :all]
   [clojure.tools.logging :as logging]
   [pallet.actions
    :refer [directory exec-checked-script exec-script remote-file]]
   [pallet.api :refer [converge lift plan-fn server-spec]]
   [pallet.blobstore :as blobstore]
   [pallet.build-actions :as build-actions :refer [build-actions]]
   [pallet.compute :as compute]
   [pallet.core :as core]
   [pallet.crate :refer [get-settings nodes-in-group]]
   [pallet.crate.automated-admin-user :as automated-admin-user]
   [pallet.crate.network-service :refer [wait-for-http-status]]
   [pallet.crate.node-js :as node-js]
   [pallet.live-test :as live-test]
   [pallet.node :refer [primary-ip]]
   [pallet.script.lib :as lib]
   [pallet.stevedore :as stevedore]
   [pallet.test-utils :refer :all]))

(deftest invoke-test
  (is (build-actions {}
        (node-js/settings {})
        (node-js/install))))

(def settings-map {})

(def nodejs-unsupported
  [])

(def node-script "
var http = require(\"http\");
http.createServer(function(request, response) {
    response.writeHead(200, {\"Content-Type\": \"text/plain\"});
    response.end(\"Hello World!\\n\");}).listen(8080);
console.log(\"Server running at http://localhost:8080/\");")


(def test-spec
  (server-spec
   :extends [(node-js/server-spec {})]
   :phases {:configure (plan-fn
                         (remote-file "node-script.js" :content node-script)
                         (exec-checked-script
                          "Run node server"
                          ("{" "nohup" ("node" "node-script.js"
                                    "2>&1 >> node.log" "&" "}"))
                          ("sleep" 5))) ; allow nohup to detach
            :verify (plan-fn
                      (wait-for-http-status
                       "http://localhost:8080/"
                       200 :url-name "node server")
                      (exec-checked-script
                       "check node-script is running"
                       ("pwd")
                       (pipe
                        ("wget" "-O-" "http://localhost:8080/")
                        ("grep" -i (quoted "Hello World")))))}
   :default-phases [:install :configure :verify]))
