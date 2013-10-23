## Usage

The node-js crate provides a `server-spec` function that returns a
server-spec. This server spec will install and run the node-js server.
You pass a map of options to configure node-js.

The `server-spec` provides an easy way of using the crate functions, and you can
use the following crate functions directly if you need to.

The `settings` function provides a plan function that should be called in the
`:settings` phase.  The function puts the configuration options into the pallet
session, where they can be found by the other crate functions, or by other
crates wanting to interact with node-js.

The `install` function is responsible for actually installing node-js.

## Live test on vmfest

For example, to run the live test on VMFest, using Ubuntu 12.04:

```sh
lein with-profile +vmfest pallet up --selectors ubuntu-12-04 --phases install,configure,test
lein with-profile +vmfest pallet down --selectors ubuntu-12-04
```
