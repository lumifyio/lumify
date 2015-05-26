Vertex Menu Plugin
=================

Plugin to add custom menu items to a vertex context menu.

To register a menu item:

        require(['configuration/plugins/graphVertexMenu/plugin'], function(GraphVertexMenuPlugin) {
             GraphVertexMenuPlugin.registerMenuItem({

                // Required Attributes
                label: (string)
                clicked: function(vertexId)
            })
        });


# Properties

* `label`: (String) The label of the menu item to add. 


# Events

* `clicked`: function(vertexId) Function that will be called when the menu item is selected. Will receive the vertex id as a parameter.


# Examples

See the folder examples for a vertex menu example.

