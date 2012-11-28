(ns pallet.crate.node-js
  "Install and configure node.js"
  (:require
   [clojure.string :as string]
   [clojure.tools.logging :as logging]
   [pallet.action.exec-script :as exec-script]
   [pallet.action.remote-directory :as remote-directory]
   [pallet.action.remote-file :as remote-file])
  (:use
   [pallet.action.package :only [install-deb packages]]
   [pallet.action.remote-directory :only [remote-directory]]
   [pallet.common.context :only [throw-map]]
   [pallet.core :only [server-spec]]
   [pallet.parameter :only [assoc-target-settings get-target-settings]]
   [pallet.phase :only [phase-fn]]
   [pallet.thread-expr :only [apply-map->]]
   [pallet.utils :only [apply-map]]
   [pallet.version-dispatch
    :only [defmulti-version-crate defmulti-version defmulti-os-crate
           multi-version-session-method multi-version-method
           multi-os-session-method]]
   [pallet.versions :only [version-string as-version-vector]]))

(def src-dir "/opt/local/node-js")

(def md5s {"0.2.1" nil})

(def ^:dynamic *nodejs-src-url* "http://nodejs.org/dist/")

(defn tarfile
  [version]
  (format "node-v%s.tar.gz" version))

(defn src-download-path [version]
  (format "%sv%s/%s" *nodejs-src-url* version (tarfile version)))


(def ^:dynamic *nodejs-defaults*
  {:version "0.6.10"})

;;; Based on supplied settings, decide which install strategy we are using
;;; for nodejs.

(defmulti-version-crate nodejs-version-settings [version session settings])

(multi-version-session-method
    nodejs-version-settings {:os :linux}
    [os os-version version session settings]
  (cond
    (:strategy settings) settings
    (:build settings) (assoc settings :strategy :build)
    (:deb settings) (assoc settings :strategy :deb)
    :else (assoc settings
            :strategy :build
            :build {:url (src-download-path (:version settings))
                    :md5 (md5s version) :unpack :tar})))

;;; ## Settings
(defn- settings-map
  "Dispatch to either openjdk or oracle settings"
  [session settings]
  (nodejs-version-settings
   session
   (as-version-vector (string/replace (:version settings) "-SNAPSHOT" ""))
   (merge *nodejs-defaults* settings)))

(defn nodejs-settings
  "Capture settings for nodejs
:version
:download
:deb"
  [session {:keys [version download deb instance-id]
            :or {version (:version *nodejs-defaults*)}
            :as settings}]
  (let [settings (settings-map session (merge {:version version} settings))]
    (assoc-target-settings session :nodejs instance-id settings)))

;;; # Install

;;; Dispatch to install strategy
(defmulti install-method (fn [session settings] (:strategy settings)))
(defmethod install-method :build
  [session {:keys [build]}]
  (->
   session
   (packages
    :yum ["gcc" "glib" "glibc-common" "python"]
    :aptitude ["build-essential" "python" "libssl-dev"])
   (apply-map-> remote-directory/remote-directory src-dir build)
   (exec-script/exec-checked-script
    "Build node-js"
    (cd ~src-dir)
    ("./configure")
    (make)
    (make install)
    (cd -))))

(defmethod install-method :deb
  [session {:keys [deb]}]
  (apply-map install-deb session "node" deb))

(defn install-nodejs
  "Install nodejs. By default will build from source."
  [session & {:keys [instance-id]}]
  (let [settings (get-target-settings
                  session :nodejs instance-id ::no-settings)]
    (logging/debugf "install-nodejs settings %s" settings)
    (if (= settings ::no-settings)
      (throw-map
       "Attempt to install nodejs without specifying settings"
       {:message "Attempt to install nodejs without specifying settings"
        :type :invalid-operation})
      (install-method session settings))))

;;; # Server spec
(defn nodejs
  "Returns a service-spec for installing nodejs."
  [settings]
  (server-spec
   :phases {:settings (phase-fn (nodejs-settings settings))
            :configure (phase-fn (install-nodejs))}))


;;; # Example
#_
(pallet.core/defnode a {}
  :bootstrap (pallet.action/phase
              (pallet.crate.automated-admin-user/automated-admin-user))
  :settings (phase-fn (nodejs-settings {}))
  :configure (pallet.action/phase
              (pallet.crate.node-js/install-nodejs)
              (pallet.crate.upstart/package)
              (pallet.action.remote-file/remote-file
               "/tmp/node.js"
               :content "
var sys = require(\"sys\"),
    http = require(\"http\");

http.createServer(function(request, response) {
    response.sendHeader(200, {\"Content-Type\": \"text/html\"});
    response.write(\"Hello World!\");
    response.close();
}).listen(8080);

sys.puts(\"Server running at http://localhost:8080/\");"
               :literal true)
              (pallet.crate.upstart/job
               "nodejs"
               :script "export HOME=\"/home/duncan\"
    exec sudo -u duncan /usr/local/bin/node /tmp/node.js 2>&1 >> /var/log/node.log"
               :start-on "startup"
               :stop-on "shutdown")))
