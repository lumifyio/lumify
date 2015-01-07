Graph Layout Plugin
=====================

Plugin to add cytoscape layouts. See example layouts: https://github.com/cytoscape/cytoscape.js/blob/v2.1.0/src/extensions/layout.null.js

To register a layout:

        require(['configuration/plugins/graphLayout/plugin'], function(GraphLayoutPlugin) {

            function MyLayout(options) {
                this.options = options;
            }

            MyLayout.prototype.run = function() {
                var cy = this.options.cy;

                // Layout nodes
                // Note: Use util/retina to convert from points to pixels (Hi-DPI displays)
                cy.nodes()[0].renderedPosition({x:100,y:100})

                // Must call ready and stop callbacks
                cy.one("layoutready", options.ready);
                cy.trigger("layoutready");

                cy.one("layoutstop", options.stop);
                cy.trigger("layoutstop");

                return this;
            };

            GraphLayoutPlugin.registerGraphLayout('myLayout', MyLayout);
        })

Remember to add a i18n value in a MessageBundle.properties. This will be displayed in the graph context menu.

        graph.layout.[Layout Registered Name].displayName=[String to display]

For example:

        graph.layout.myLayout.displayName=My Layout
