(ns pallet.crate.node-js
  "Install and configure node.js"
  (:require
   [clojure.string :as string]
   [clojure.tools.logging :refer [debugf]]
   [pallet.action :refer [with-action-options]]
   [pallet.actions
    :refer [exec-checked-script packages remote-directory remote-file]]
   [pallet.common.context :refer [throw-map]]
   [pallet.api :as api :refer [plan-fn]]
   [pallet.crate :refer [assoc-settings defmethod-plan defplan get-settings]]
   [pallet.crate-install :as crate-install]
   [pallet.utils :refer [apply-map deep-merge]]
   [pallet.version-dispatch
    :refer [defmethod-version-plan defmulti-version-plan]]
   [pallet.versions :refer [version-string as-version-vector]]))

(def facility :nodejs)

(def ^:dynamic *nodejs-defaults*
  {:version "0.6.10"})

;;; # Build helpers

(def ^:dynamic *nodejs-src-url* "http://nodejs.org/dist/")
(def src-dir "/opt/local/node-js")
(def md5s {"0.2.1" nil})

(defn tarfile
  [version]
  (format "node-v%s.tar.gz" version))

(defn src-download-path [version]
  (format "%sv%s/%s" *nodejs-src-url* version (tarfile version)))

;;; # Settings
;;;
;;; We install from package manager if possible, and fall back to
;;; building from source
;;;
;;; Links:
;;; https://github.com/joyent/node/wiki/Installing-Node.js-via-package-manager

(defmulti-version-plan default-settings [version])

;; If no distro specific method, build node-js from source
(defmethod-version-plan
  default-settings {:os :linux}
  [os os-version version]
  {:version version
   :install-strategy ::build
   :src-dir src-dir
   :build {:url (src-download-path version)
           :md5 (md5s version) :unpack :tar}})

;; on Ubuntu, we can install form PPA, as the distro version is old
(defmethod-version-plan
  default-settings {:os :ubuntu}
  [os os-version version]
  {:version version
   :install-strategy :package-source
   :package-source {:name "ppa:chris-lea"
                    :aptitude
                    {:url "ppa:chris-lea/node.js"}}
   :packages ["nodejs"]})

;; on amazon linux, we can install from the already installed epel
(defmethod-version-plan
  default-settings {:os :amzn-linux}
  [os os-version version]
  {:version version
   :install-strategy :packages
   :packages ["nodejs" "npm"]
   :package-options {:enable "epel"}})

;; on Redhat based distros, we can install form EPEL
(defmethod-version-plan
  default-settings {:os :rh-base}
  [os os-version version]
  {:version version
   :install-strategy :package-source
   :repository {:id :epel
                :version "6.8"}
   :packages ["nodejs" "npm"]})

;; on fedeora, we can install from yum
(defmethod-version-plan
  default-settings {:os :fedora}
  [os os-version version]
  {:version version
   :install-strategy :packages
   :packages ["nodejs"]})

;; on suse, we can install from zypper
(defmethod-version-plan
  default-settings {:os :suse}
  [os os-version version]
  {:version version
   :install-strategy :packages
   :packages ["nodejs"]})

;; on arch distros, we can install from pacman
(defmethod-version-plan
  default-settings {:os :arch-base}
  [os os-version version]
  {:version version
   :install-strategy :packages
   :packages ["nodejs"]})

;; on gentoo distros, we can install from emerge
(defmethod-version-plan
  default-settings {:os :arch-base}
  [os os-version version]
  {:version version
   :install-strategy :packages
   :packages ["nodejs"]})

;; on osx, we can install from brew
(defmethod-version-plan
  default-settings {:os :arch-base}
  [os os-version version]
  {:version version
   :install-strategy :packages
   :packages ["nodejs"]})

;;; ## Settings
(defn settings
  "Capture settings for nodejs"
  [{:keys [version instance-id]
    :or {version (:version *nodejs-defaults*)}
    :as settings}]
  (let [settings (deep-merge (default-settings version)
                             (dissoc settings :instance-id))]
    (debugf "node-js settings %s" settings)
    (assoc-settings facility settings {:instance-id instance-id})))

;;; # Install

(defmethod-plan crate-install/install ::build
  [facility instance-id]
  (let [{:keys [build src-dir]} (get-settings
                                 facility {:instance-id instance-id})]
    (packages
     :yum ["gcc" "glib" "glibc-common" "python"]
     :aptitude ["build-essential" "python" "libssl-dev"])
    (apply-map remote-directory src-dir build)
    (with-action-options {:script-dir src-dir}
      (exec-checked-script
       "Build node-js"
       ("./configure")
       ("make")
       ("make" install)))))

(defplan install
  [{:keys [instance-id]}]
  (crate-install/install facility instance-id))

;;; # Server spec
(defn server-spec
  "Returns a service-spec for installing nodejs."
  [{:keys [instance-id] :as settings}]
  (api/server-spec
   :phases {:settings (plan-fn
                       (pallet.crate.node-js/settings settings))
            :install (plan-fn
                       (install {:instance-id instance-id}))}))
