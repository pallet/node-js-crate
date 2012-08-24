(ns pallet.crate.node-js-test
  (:use
   [pallet.action :only [def-clj-action]]
   [pallet.action.exec-script :only [exec-checked-script exec-script]]
   [pallet.action.package :only [install-deb package]]
   [pallet.action.remote-file :only [remote-file]]
   [pallet.node :only [primary-ip]]
   [pallet.parameter :only [get-target-settings]]
   [pallet.parameter-test :only [settings-test]]
   [pallet.session :only [nodes-in-group]]
   clojure.test
   pallet.test-utils)
  (:require
   [clojure.tools.logging :as logging]
   [pallet.action :as action]
   [pallet.action.directory :as directory]
   [pallet.action.exec-script :as exec-script]
   [pallet.action.file :as file]
   [pallet.action.package :as package]
   [pallet.action.remote-file :as remote-file]
   [pallet.blobstore :as blobstore]
   [pallet.build-actions :as build-actions]
   [pallet.compute :as compute]
   [pallet.core :as core]
   [pallet.crate.automated-admin-user :as automated-admin-user]
   [pallet.crate.node-js :as node-js]
   [pallet.crate.network-service :as network-service]
   [pallet.live-test :as live-test]
   [pallet.parameter :as parameter]
   [pallet.parameter-test :as parameter-test]
   [pallet.phase :as phase]
   [pallet.script.lib :as lib]
   [pallet.session :as session]
   [pallet.stevedore :as stevedore]
   [pallet.thread-expr :as thread-expr]))

(deftest invoke-test
  (is (build-actions/build-actions
       {}
       (node-js/nodejs-settings {})
       (node-js/install-nodejs))))

(def settings-map {})

(def nodejs-unsupported
  [])

(def node-script "
var http = require(\"http\");
http.createServer(function(request, response) {
    response.writeHead(200, {\"Content-Type\": \"text/plain\"});
    response.end(\"Hello World!\\n\");}).listen(8080);
console.log(\"Server running at http://localhost:8080/\");")

(deftest live-test
  (live-test/test-for
   [image (live-test/exclude-images (live-test/images) nodejs-unsupported)]
   (live-test/test-nodes
    [compute node-map node-types]
    {:nodejs
     {:image image
      :count 1
      :phases {:bootstrap (phase/phase-fn
                           (automated-admin-user/automated-admin-user))
               :settings (phase/phase-fn
                          (node-js/nodejs-settings settings-map))
               :configure (phase/phase-fn
                            (node-js/install-nodejs)
                            (remote-file
                             "node-script.js"
                             :content node-script)
                            (exec-script
                             ("nohup" (node "node-script.js"
                                            "2>&1 >> /var/log/node.log" "&"))))
               :verify (phase/phase-fn
                        (network-service/wait-for-http-status
                         "http://localhost:8080/"
                         200 :url-name "node server")
                        (exec-script/exec-checked-script
                         "check node-script is running"
                         (pipe
                          (wget "-O-" "http://localhost:8080/")
                          (grep -i (quoted "Hello World")))))}}}
    (core/lift (:nodejs node-types) :phase :verify :compute compute))))
