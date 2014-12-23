Graph Selector Plugin
=====================

Plugin to add custom node selection menu items. Graph provides Select All, None, and Invert by default.

To register a selector:

        require(['configuration/plugins/graphSelector/plugin'], function(GraphSelectorPlugin) {

            // Randomly select a node
            GraphSelectorPlugin.registerGraphSelector('myRandomSelector', function(cy) {
                var nodes = cy.nodes().unselect(),
                    randomIndex = Math.floor(Math.random() * nodes.length);

                nodes[randomIndex].select();
            });
        })

Remember to add a i18n value in a MessageBundle.properties. This will be displayed in the graph context menu.

        graph.selector.[Selector Registered Name].displayName=[String to display]

For example:

        graph.selector.myRandomSelector.displayName=Random Node
