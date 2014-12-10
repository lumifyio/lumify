
Graph Style Plugin
=====================

Plugin to configure the cytoscape stylesheet

To register a styler:

        require(['configuration/plugins/graphStyle/plugin'], function(GraphStylePlugin) {
            GraphStylePlugin.registerGraphStyler(function(cytoscapeStylesheet) {

                // Changes selected nodes color to red
                cytoscapeStylesheet.selector('node:selected')
                    .css({
                        color: '#FF0000'
                    })
            });
        })

