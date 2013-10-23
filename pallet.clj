;;; Pallet project configuration file

(require
 '[pallet.crate.node-js-test :refer [test-spec]]
 '[pallet.crates.test-nodes :refer [node-specs]])

(defproject node-js-crate
  :provider node-specs                  ; supported pallet nodes
  :groups [(group-spec "node-js-live-test"
             :extends [with-automated-admin-user
                       test-spec]
             :roles #{:live-test :default})])
