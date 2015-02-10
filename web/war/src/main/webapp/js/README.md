
# JavaScript Source Tree

The `index.hbs` page loads [require.js](http://requirejs.org) with the configuration in `require.config.js` and loads subsequently loads `lumify.js`. This is the main entry point to Lumify.

Lumify then transitions into one of three possible states based on url fragment identifier and cookies:

* Login
* Workspace
* Fullscreen

## Login

File: `login.js`

The login page is shown when the users session is expired or missing. It renders the login page along with the authentication plugin to login the user, at which point it transitions to the Workspace or Fullscreen view. 


## Workspace

File: `app.js`

The workspace view of Lumify is the default view. It contains the main application, with a graph, map, detail pane, and ancillary panes.

If the fragment identifier contains `#w=`, the user is prompted to add entities to their workspace after load.

        https://try.lumify.io#w=[ vertexId_1 [, vertexId_2 ] ]

## Fullscreen

File: `appFullscreenDetails.js`

The fullscreen view is a view of one or many entities in a grid. The entities are displayed using the same component as they are shown in the workspace when selecting them. The entities loaded are loaded using the fragment identifier.

        https://try.lumify.io#v=[ vertexId_1 [, vertexId_2 ] ]
