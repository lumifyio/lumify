Graph View Plugin
=================

Plugin to add custom view components which overlay the graph. Used for toolbars, etc., that interact with the graph.
Views are always [Flight](https://github.com/flightjs/flight) components.

To register a view:

        require(['configuration/plugins/graphView/plugin'], function(GraphViewPlugin) {

            // Define a custom Flight component
            define('myplugins/hello_world', ['flight/lib/component'], function(defineComponent) {
                return defineComponent(HelloWorld);

                function HelloWorld() {
                    this.after('initialize', function() {
                        this.$node.html('Hello World!!');
                    })
                }
            });

            // Register the component path, and optionally, provide a CSS class to ease styling
            // The plugin framework will add a div element with this class to the graph DOM.
            // The Java WebAppPlugin subclass can register a CSS file to supply styling for this element.
            GraphViewPlugin.registerGraphView('myplugins/hello_world', 'hello-world-example');
        });

All graph views are placed just above the graph in an absolutely positioned container element. This container is automatically resized based on open panes. To position elements use absolute positioning relative to the container. For example, to position a view in the top right, using inline css: (or define in external stylesheet using the className)

        this.$node.html('Hello World').css({
            position: 'absolute',
            // Move to right of graph view tool items (zoom, fit, pan, etc)
            right: '100px',
            top: '20px'
        })

