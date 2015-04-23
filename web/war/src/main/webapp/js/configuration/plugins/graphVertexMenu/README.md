Vertex Menu Plugin
=================

Plugin to add custom menu item to a vertex context menu.

To register a menu item:

        require(['configuration/plugins/graphVertexMenu/plugin'], function(GraphVertexMenuPlugin) {
             GraphVertexMenuPlugin.registerMenuItem(options)
        });


Possible options are:

label:string
clicked:function(vertexId)
shouldDisable:function(selection, vertexId, target)
