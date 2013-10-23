[Repository](https://github.com/pallet/node-js-crate) &#xb7;
[Issues](https://github.com/pallet/node-js-crate/issues) &#xb7;
[API docs](http://palletops.com/node-js-crate/0.8/api) &#xb7;
[Annotated source](http://palletops.com/node-js-crate/0.8/annotated/uberdoc.html) &#xb7;
[Release Notes](https://github.com/pallet/node-js-crate/blob/develop/ReleaseNotes.md)

A [pallet](http://palletops.com/) crate to install and configure
 [node-js](http://nodejs.org).

### Dependency Information

```clj
:dependencies [[com.palletops/node-js-crate "0.8.0-SNAPSHOT"]]
```

### Releases

<table>
<thead>
  <tr><th>Pallet</th><th>Crate Version</th><th>Repo</th><th>GroupId</th></tr>
</thead>
<tbody>
  <tr>
    <th>0.8.0-RC.3</th>
    <td>0.8.0-SNAPSHOT</td>
    <td>clojars</td>
    <td>com.palletops</td>
    <td><a href='https://github.com/pallet/node-js-crate/blob/0.8.0-SNAPSHOT/ReleaseNotes.md'>Release Notes</a></td>
    <td><a href='https://github.com/pallet/node-js-crate/blob/0.8.0-SNAPSHOT/'>Source</a></td>
  </tr>
</tbody>
</table>

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

## License

Copyright (C) 2012, 2013 Hugo Duncan

Distributed under the Eclipse Public License.
